# Speech enhancement (DeepFilterNet 3, native)

Native C++20 implementation of the DeepFilterNet 3 DSP pipeline, ONNX Runtime inference
and streaming engine. The Android module builds `libspeech_enhancement.so`; the DSP and
enhancer layers are platform independent and are unit tested on a desktop host.

Upstream references (studied behaviour, not translated line by line):

| Upstream file                             | Used for                                                               |
|-------------------------------------------|------------------------------------------------------------------------|
| `libDF/src/lib.rs`                        | STFT/ISTFT, Vorbis window, ERB filterbank, features, post filter       |
| `libDF/src/tract.rs`                      | streaming orchestration, rolling buffers, deep filtering, stage gating |
| `DeepFilterNet/df/config.py`              | `config.ini` parameters and defaults                                   |
| `DeepFilterNet/df/deepfilternet3.py`      | encoder/decoder architecture and tensor layouts                        |
| `DeepFilterNet/df/enhance.py`, `df/io.py` | offline pipeline, PCM16 conventions, resampling                        |

## Layout

```text
src/main/cpp/
├── CMakeLists.txt            single project: Android entry point and host test build
├── cmake/Config.cmake        PFFFT, ONNX Runtime, GoogleTest + target setup
├── dsp/                      signal processing, no Android / JNI / ONNX
│   ├── include/              audio_frame, audio_buffer, window, stft, istft, fft,
│   │                         erb, dsp_features, deep_filter, resampler
│   ├── src/
│   └── test/                 GoogleTest suites + reference vectors
├── enhancer_core/            DeepFilterNet 3 orchestration
│   ├── include/              enhancer_config, enhancer_state, enhancer_session,
│   │                         model_config, onnx_backend, deepfilter_engine
│   ├── src/
│   └── test/                 orchestration, streaming, ONNX and end-to-end suites
├── enhancer_jni/             Android JNI bridge (the only JNI-aware target)
│   ├── include/              enhancer_jni, jni_utils (recorder-native copy),
│   │                         speech_enhancer_manager
│   ├── src/                  enhancer_jni, speech_enhancer_manager, enhancer_jni_interop
│   └── test/                 on-device smoke tests
└── test/support/             shared fixtures (reference loader, stub runner)
└── (see onnx-tooling/ at the module root for the Python model-preparation tooling)
```

Every layer owns its own `include/`, `src/` and `test/`; test cases are not collected in a
single top-level package. Only module-independent fixtures live in `test/support/`.

The Python model tooling lives outside the source tree, in
`data/speech-enhancements/onnx-tooling/` (graph preparation and the streaming reference).

`dsp_features.h` carries the module prefix on purpose: as a bare `features.h` it shadowed the
system `<features.h>` that libc++ includes, which broke the Android build.

Two library targets keep the ONNX dependency isolated:

* `dfn_enhancer_core` — pipeline, streaming state, session. No ONNX Runtime.
* `dfn_enhancer_onnx` — `onnx_backend_runner` (only built when ONNX Runtime is available).

`speech_enhancement` (JNI) links both, plus `dsp`.

## Processing pipeline

Per hop (`hop_size` = 480 samples, 48 kHz):

1. STFT (Vorbis window, 960-point real FFT via PFFFT), analysis normalization `2·hop/N²`.
2. ERB features (32 bands, dB, exponential mean normalization, α = 0.99) and normalized
   complex features for the first 96 bins.
3. Encoder → local SNR, embedding, encoder taps, DF input features.
4. Stage selection from the local SNR (libDF defaults): `< -10 dB` zero mask,
   `> 30 dB` bypass, `> 20 dB` ERB gains only, otherwise ERB gains + deep filtering.
5. Deep filtering of the first 96 bins over 5 frames; optional post filter and attenuation
   limit.
6. ISTFT with overlap-add.

The output is delayed by `latency_samples()` = `lookahead·hop + (fft − hop)` = **1440
samples (30 ms)**. `EnhancerSession::flush()` pads the tail so an offline stream can be
delay-compensated.

## Models

The graphs from `models/DeepFilterNet3_onnx.tar.gz` cannot be used frame by frame as
shipped (their GRU state resets on every `Run()` and their causal convolutions rely on
zero padding). `onnx-tooling/prepare_models.py` derives streaming graphs with explicit
recurrent state inputs/outputs and per-frame outputs, and verifies them against the
originals (bit-exact on ONNX Runtime):

```bash
python onnx-tooling/prepare_models.py --model-dir <enc/erb_dec/df_dec .onnx> --out-dir <out>
```

`onnx-tooling/reference_streaming.py` implements the same pipeline in Python and is the
executable specification for the C++ engine; it reproduces the official offline `enhance()`
output up to the documented algorithmic delay (RMS error −33 dB relative to the signal).

## Testing

```bash
# DSP + orchestration suites (no ONNX Runtime required)
cmake -S src/main/cpp -B build/dfn-tests -DCMAKE_BUILD_TYPE=Release -DDFN_BUILD_TESTS=ON
cmake --build build/dfn-tests -j
ctest --test-dir build/dfn-tests --output-on-failure
```

Optional ONNX Runtime and end-to-end tests:

```bash
cmake -S src/main/cpp -B build/dfn-tests -DDFN_BUILD_TESTS=ON \
  -DDFN_ONNXRUNTIME_ROOT=<ORT host distribution> \
  -DDFN_TEST_MODEL_DIR=<prepared models> \
  -DDFN_TEST_ENHANCER_DATA=<generated reference vectors>
```

Reference vectors are not checked in (see `.gitignore`); generate them with
`onnx-tooling/generate_reference.py` (uses the official `libdf`). Tests that need them
skip automatically when the files are absent.

The Kotlin instrumentation tests read their model graphs and audio clips from
`src/androidTest/assets/` (also gitignored). Populate that folder once with the module-level
script — it downloads the raw graphs from Hugging Face, converts them to streaming graphs
through an ephemeral `uv` environment, and fetches the Valentini speech clips:

```bash
./build_tests_assets.sh          # add --force to re-download / re-convert
```

To listen to the results, the `dumpEnhancedAudio` task renders the noisy / clean / enhanced
WAV of every clip and pulls them off the device:

```bash
./gradlew :data:speech-enhancements:dumpEnhancedAudio -PdumpName=baseline
./gradlew :data:speech-enhancements:dumpEnhancedAudio -PdumpName=adaptive -PdumpAdaptive=true
./gradlew :data:speech-enhancements:dumpEnhancedAudio \
  -PdumpName=adaptive_al12 -PdumpAdaptive=true -PdumpAttenuationLimitDb=12
```

Each run lands in `build/enhanced-dumps/<dumpName>/`. `dumpAdaptive` (off),
`dumpPostFilterBeta` (0.0) and `dumpAttenuationLimitDb` (100) mirror `EnhanceOptions`.

## Android build

```bash
./gradlew :data:speech-enhancements:externalNativeBuildRelease
```

PFFFT v1.3.1 is fetched with `FetchContent` and uses NEON on ARM ABIs and SSE on x86 (selected per ABI by PFFFT's own
probe; nothing is forced). ONNX Runtime 1.32.0 is fetched
as the official `onnxruntime-android` AAR from Maven Central, checksum-verified, and
imported per ABI; the AAR ships `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`.

## Status

Implemented: DSP foundation, feature extraction, deep filtering, resampling, model
configuration, ONNX backend, streaming engine, session lifecycle, and the JNI bridge.
The bridge is the recorder-native shape: `enhancer_jni_interop.cpp` contains nothing but
the `Java_…` entry points, and each one unwraps a handle, calls a single
`speech_enhancer_manager` method and translates a thrown error into a Java exception. A
handle owns a heap `std::shared_ptr<SM>` (all state per instance, nothing static), so
managers can be shared with other native owners and die when the last reference goes. The
Kotlin wrapper is `ClearSpeechEnhancer` over `IClearSpeechEnhancer`.

Each `enhance()` call is an offline pass (`process` → `flush` → drop the algorithmic
latency → strength blend) that consumes the session's recurrent state, so a second call
without an intervening `reset()` keeps streaming from where the previous call ended. The
Kotlin wrapper enforces one `enhance()` per `reset()`: `enhance()` throws
`SpeechEnhancerStateException` until `reset()` restores the session (engine, splitter,
pending frames and stats) to a clean state. `reset()` is idempotent, works at any point in
the instance's life after `create()`, and returns whether an actual reset happened —
`true` when a consumed session was restored, `false` when there was nothing to reset.

The final blend can be either a constant (`EnhanceOptions.strength`) or SNR-adaptive
(`EnhanceOptions.adaptiveStrength`): the manager then picks the blend per hop from the
session's per-frame local SNR estimate, keeping the full model output on noisy frames and
fading back towards the input on already-clean ones. The ONNX model over-suppresses clean
speech (up to ~10 dB of SNR on the Valentini clips) and this recovers most of it without
losing the denoising on noisy input.

Two model-side stages are exposed per call through `EnhanceOptions` and applied by the
manager before the pass: `postFilterBeta` (Valin post-filter; `0.0` disables, the official
Python CLI uses `0.02`) and `attenuationLimitDb` (mixes the output back towards the input so
noise is never attenuated by more than that; `>= 100.0` disables). Both default to the
engine's previous behaviour (off), so enabling them is an explicit choice.

The prepared
streaming graphs are loaded lazily on the first enhance; without them every call fails
closed with an exception instead of crashing. Resampling in and out of the 48 kHz
pipeline is a later phase, so the bridge rejects other sample rates.
