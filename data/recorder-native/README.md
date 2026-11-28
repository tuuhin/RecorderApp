# recorder-native

Native audio recording for RecorderApp. The module exposes one Kotlin class per container format,
`WavAudioRecorder` and `FlacAudioRecorder`, behind an identical
`CustomRecorder` API. Everything below that line is C++ reached over JNI: audio capture with Oboe's default backend, a
lock-free
SPSC ring buffer, a worker thread, and a per-format file writer. `oboe` and
`libFLAC` are built from source at CMake configure time, so the module ships no prebuilt binaries.

## Layout

```
src/main/cpp/
├── CMakeLists.txt            # entry point; restores CMAKE_SYSTEM_VERSION for NDK r30 + armeabi-v7a
├── cmake/Config.cmake        # FetchContent: oboe 1.11.0, libFLAC 1.5.0 (source, git-tag pinned)
├── recorder_common/          # INTERFACE library (header only)
│   └── include/
│       ├── audio_recorder.h  # audio_recorder<Writer> template: the whole lifecycle
│       ├── spsc_ring_buffer.h# single-producer / single-consumer ring buffer
│       ├── jni_utils.h       # handle lookup, exception translation, UtfChars
│       ├── handle_registry_manager.h # opaque sequential-id handle registry (thread-safe)
│       └── recorder_config.h # validated capture config (rate, channels, 16-bit PCM)
├── custom_wav/
│   ├── include/wav_file_writer.h
│   └── src/                  # wav_file_writer.cpp (RIFF writer) + wav_jni_interop.cpp
└── custom_flac/
    ├── include/flac_file_writer.h
    └── src/                  # flac_file_writer.cpp (libFLAC writer) + flac_jni_interop.cpp
```

The two `custom_*` directories contain only a file writer and a JNI interop layer. All shared logic (lifecycle, capture,
buffering, error handling) lives once in
`recorder_common`, and each format instantiates the same template with its own writer type, so
nothing is copied between formats.

## The API contract

`WavAudioRecorder` and `FlacAudioRecorder` behave identically; only the produced file differs. Both
load their own shared library (`System.loadLibrary("wav_recorder")` /
`System.loadLibrary("custom_flac")`) and expose matching JNI entry points:

```
create()            allocate the native recorder, returns a handle
release()           free the native recorder (idempotent, call-twice-safe)
setOutputFile(File) set the output path (must precede prepare())
setRecorderConfig(RecorderConfig(sampleRate, channel))
prepare()           open the oboe stream and the output file
start()             begin capturing (throws before prepare, when already recording)
stop()              stop capturing, finalize the file (idempotent)
pause() / resume()  stop and reopen input stream while preserving the output
reset()             release everything, clear path and config, record again
isRecording          state flag, throws before create()
droppedFrames        cumulative input frames not accepted by the native buffer
onError              optional callback for failures; the original call still throws
```

Lifecycle misuse surfaces as the standard Kotlin exceptions (`IllegalStateException`,
`IllegalArgumentException`) plus the module's own
`RecorderHandleException`, `RecorderAlreadyInitiatedException`, and
`InvalidHandleResultException`. Native `std::invalid_argument` /
`std::logic_error` are translated to those at the JNI boundary in
`recorder_common/include/jni_utils.h`.

JNI handles are opaque sequential ids minted by the header-only
`recorder_common/include/handle_registry_manager.h` and passed as `Long`. Zero is the only sentinel used on both
sides; it always means "no recorder". A handle is only trusted if the registry still holds it: a stale, reused, or
forged id resolves to `nullptr` and surfaces as `IllegalArgumentException` at the boundary, so it can never be
reinterpreted as a pointer (type confusion / use-after-free). Never validate a handle with `<= 0`; only `0` means
"no recorder".

### Why the lifecycle methods are not `@FastNative`

`@FastNative` (like the older `!bang` JNI) skips the thread-state transition checks around a native call. That
transition is exactly what keeps the VM responsive while a call runs, so ART documents two hard requirements for a
`@FastNative` method: it must **never call back into managed code**, and it must **never block**. Violating either
can hang the entire VM transition, deadlock GC, or leave other threads stuck waiting for the calling thread to
re-enter managed code.

Reviewing the recorder's native entry points against those rules:

- `isRecordRunning` reads one atomic flag and returns. No callbacks, no I/O, no locks worth mentioning. This is the
  one legitimate use, and it keeps `@FastNative` for the latency-sensitive polling paths.
- `stopRecorder` drains the ring buffer through the worker thread and finalizes the output file — disk I/O that can
  block for the duration of a flush.
- `cancelRecorder` deletes the output file; `resetRecorder` finalizes any open recording. Both are file operations.
- `pauseRecorder` / `resumeRecorder` close and reopen the Oboe input stream, which joins the audio callback thread
  and can block on driver work.

Annotating any of those with `@FastNative` would block the VM transition while holding none of the usual safeties,
for no measurable gain: these calls run a handful of times per recording, not per audio callback. The annotations
were therefore removed from every blocking lifecycle method and kept only on `isRecordRunning`. The same review
removed `@FastNative` from the compression module's LZ4 entry points, which call back into Java to read the
buffers' `position`/`limit`.

## How capture works (NDK + oboe)

`audio_recorder<Writer>` owns the pipeline:

```
oboe callback thread          worker thread
     onAudioReady()                worker_loop()
          │ write                      │ read (up to 1024 frames)
          ▼                            ▼
   SPSC ring buffer ─────────────► Writer::write() ──► file
   (2 seconds of capacity)         (1 ms poll when idle)
```

- **prepare ()** builds the oboe `AudioStreamBuilder`: `Direction::Input`, Oboe's default
  backend, `AudioFormat::I16`, the configured sample rate and channel count,
  `PerformanceMode::None`, `SharingMode::Shared`, `InputPreset::Generic`, and both the data and
  error callbacks. After opening, the recorder reads the *actual* stream rate, channel count, and
  format and uses those values for the writer, so the file metadata describes captured PCM. The
  default request is 48 kHz; devices may negotiate another valid rate, which is logged explicitly.
  The recorder first opens with Oboe's default backend. If that open fails, it retries the same
  configuration with `OpenSLES`, which is generally more tolerant of the app's parallel Java
  `AudioRecord` amplitude visualizer. Logs identify the selected backend, and when both attempts
  fail the error includes the fallback failure instead of silently continuing with an unusable
  stream. The focused module tests do not start the app's parallel visualizer, so paired app
  recording remains the decisive validation.
- **start ()** launches the worker thread, arms the accepting flag, and calls
  `requestStart()`.
- **onAudioReady ()** is the single producer: it copies the callback's frames into the ring buffer
  and counts anything the buffer could not take as dropped frames.
- **worker_loop ()** is the single consumer: it drains the ring buffer in 1024-frame chunks into the
  writer, sleeping 1 ms when empty.
- **stop ()** requests a stream stop, closes the stream (so the callback is provably dead), joins
  the worker (which drains the buffer), and finalizes the writer. Calling stop again is a no-op, and
  stop before prepare is silent.
- Worker and oboe errors are retained and rethrown at the next lifecycle boundary; Kotlin invokes
  `onError` before rethrowing. The handler itself cannot suppress or replace the original failure.
- `droppedFrames` reports the cumulative number of input frames the callback could not enqueue.
- **pause () / resume ()** stop and close the input stream on pause, then reopen it using the same
  actual format on resume. If the device negotiates a different rate, channel count, or sample
  format, resume fails instead of appending incompatible PCM. The worker and writer remain alive,
  so compatible resumed audio appends to the same output; samples during the pause are not captured.

## How the files are written

**WAV (`custom_wav`)** writes a canonical 44-byte RIFF header when the file is opened, appends raw
16-bit PCM as the worker drains, and on finalize seeks back to patch the RIFF size (offset 4) and
the data-chunk size (offset 40). The result is a standard PCM WAV that any player and media3's
`WavExtractor` parse directly. The writer uses C stdio (`FILE*`, `fwrite`, and `fseek`) rather
than `std::fstream`: it only needs sequential writes and two header seeks, while iostreams pull
in substantial libc++ stream/locale code. This changes the I/O mechanism, not the WAV bytes or
recording pipeline.

**FLAC (`custom_flac`)** drives libFLAC's stream encoder: configured from the same `recorder_config`
(channels, 16-bit depth, sample rate, compression level 5), fed
`FLAC__stream_encoder_process_interleaved` with interleaved signed 16-bit samples, and finished
with `FLAC__stream_encoder_finish()`, which rewrites the STREAMINFO block with the final statistics (total samples,
MD5). A zero-frame FLAC produced by an immediate stop is still a valid, finalized
container.

## Build and dependencies

- Both native dependencies are pulled by CMake `FetchContent` from their git tags and compiled from
  source: **oboe 1.11.0** (audio) and **libFLAC 1.5.0**
  (FLAC, built static with programs, examples, testing, docs, OGG and C++ bindings disabled). No
  `.a`/`.so` is ever checked in.
- `cmake/patches/remove_playback_parameters_stream_format.cmake` applies a guarded patch to
  Oboe's `AudioStreamAAudio::setPlaybackParameters` diagnostic. On invalid parameters Oboe logs
  the error without formatting the values through `std::stringstream`; that diagnostic-only
  formatting kept iostream/locale code reachable in each recorder library. CMake fails if the
  expected upstream call changes, so review the patch when upgrading Oboe.
- Recorder libraries keep the default NDK `c++_static` runtime and speed-oriented compile flags.
  A whole-app `c++_shared` experiment increased the APK size, so do not enable it for this module
  alone. The WAV stdio change and Oboe diagnostic patch reduce linked support code without applying
  size-oriented optimization to the capture/encoding path.
- ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64` (x86 is intentionally dropped).
- The root `CMakeLists.txt` restores `CMAKE_SYSTEM_VERSION` after `project()`:
  NDK r30's legacy toolchain resets it to 1 with the pinned CMake 3.22.1, which made libFLAC's
  `fseeko` probe fail on `armeabi-v7a`.
- Release builds minify, and JNI binding is name-based: the keep rules live in
  `src/main/keepRules/rules.keep`. Dropping them surfaces as
  `UnsatisfiedLinkError` in the release app.

## Testing

34 instrumented tests in `WavAudioRecorderTest` and `FlacAudioRecorderTest` cover lifecycle contracts,
before-create guards, invalid configs, native error callbacks, dropped-frame access, concurrent
release, resumed sample continuity, reset to a new file, release-and-record-again, and immediate stop.

```
ANDROID_SERIAL=<device> ./gradlew :data:recorder-native:connectedDebugAndroidTest
```

- Recording windows are real wall-clock time (`runBlocking` + `delay`); do not convert them to
  `runTest`, whose virtual time advances instantly and would record nothing.
- Files are validated two ways: media3 extractors for the container contract (track count, format,
  `seekMap.durationUs`), and direct header parsing for the true sample count: the WAV data-chunk
  size for WAV, and the STREAMINFO total-samples field for FLAC, each bounded against a quarter
  second of audio.
- Pitfall worth remembering: media3's `FakeTrackOutput.sampleCount` counts extractor-emitted chunks,
  not audio samples (roughly 8 KB per chunk for
  `WavExtractor`, one per ~4096-sample frame for `FlacExtractor`). Bound it against a sample rate
  and healthy recordings fail with `14 >= 11025`.
- media3's `FlacExtractor` cannot parse a zero-frame FLAC, so the FLAC immediate-stop test validates
  the container structure directly (magic, STREAMINFO block, declared stream description) instead of
  extracting.

## Known limitations

- Dropped-frame reporting is cumulative and read on demand; it does not currently provide a push
  notification when the count increases.
- Only 16-bit PCM is supported in either format; `recorder_config` validates and both writers reject
  anything else.
