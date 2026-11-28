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

The converted streaming graphs are redistributed on Hugging Face at
[`tuuhin/deepfilternet3-onnx`](https://huggingface.co/tuuhin/deepfilternet3-onnx)
(`config.ini`, `enc.onnx`, `erb_dec.onnx`, `df_dec.onnx`; dual-licensed MIT / Apache-2.0,
with provenance in that repository's `NOTICE`). The app downloads them at runtime: the exact
URLs, sizes and SHA-256 checksums live in `src/main/assets/models.json`. `SpeechModelReader`
checks the installed files against that manifest (present / valid / corrupted) and
`SpeechModelDownloader` fetches and verifies them before installing under `filesDir`.

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
script — it downloads the official upstream graphs (pinned commit, SHA-256 verified), converts
them to streaming graphs through an ephemeral `uv` environment, and fetches the Valentini
speech clips:

```bash
./build_tests_assets.sh          # add --force to re-download / re-convert
```

## Android build

```bash
./gradlew :data:speech-enhancements:externalNativeBuildRelease
```

PFFFT v1.3.1 is fetched with `FetchContent` and uses NEON on ARM ABIs and SSE on x86 (selected per ABI by PFFFT's own
probe; nothing is forced). ONNX Runtime 1.32.0 is fetched
as the official `onnxruntime-android` AAR from Maven Central, checksum-verified, and
imported per ABI; the AAR ships `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`.

Android JNI linking enables `--gc-sections`, `--exclude-libs,ALL`, ThinLTO, hidden visibility
and `--icf=safe`. The latter folds identical functions only when their addresses are not
observable; it is enabled only for Android/ELF, not host builds. Do not copy speed-oriented
flags such as `-O3` or loop unrolling without measuring the packaged release libraries: the
ONNX Runtime `.so` is prebuilt and cannot be reduced by these target-local options.

## Status

Implemented: DSP foundation, feature extraction, deep filtering, resampling, model
configuration, ONNX backend, streaming engine, session lifecycle, and the JNI bridge.
The bridge is the recorder-native shape: `enhancer_jni_interop.cpp` contains nothing but
the `Java_…` entry points, and each one unwraps a handle, calls a single
`speech_enhancer_manager` method and translates a thrown error into a Java exception. A
handle owns a heap `std::shared_ptr<SM>` (all state per instance, nothing static), so
managers can be shared with other native owners and die when the last reference goes. The
Kotlin wrapper is `ClearSpeechEnhancer` over `IClearSpeechEnhancer`.

Each `process()` call feeds one chunk of any size and emits the enhanced samples that are
ready, so the concatenated output across the stream is sample-aligned with the input;
`finish()` flushes the tail so the stream's total output equals its total input. `reset()`
clears all stream state and is idempotent, so it works at any point in the instance's life.

## Tuning the output

Every knob lives in `EnhanceOptions` (Kotlin, supplied per `process`/`finish` call). The
engine's own defaults sit in `enhancer_config.h`; the Kotlin layer overrides `postFilterBeta`
to `0.02`. Defaults below are the Kotlin defaults.

| Option                      | Default       | What it holds                                                                                                                                                                    | Effect of changing it                                                                                                                                                                     |
|-----------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `blending.strength`         | `1.0`         | Linear blend of the model output (`1.0`) against the original input (`0.0`).                                                                                                     | Lower keeps more of the original: less denoising, but fewer suppression artifacts. `0.0` is a passthrough, `0.7` is a mild "half-enhanced" sound.                                         |
| `blending.adaptive`         | `false`       | Per-frame blend driven by the engine's local-SNR estimate: noisy frames keep the full model output, already-clean frames fade back to the input. `strength` becomes the ceiling. | `true` protects clean speech from over-suppression (the model otherwise costs up to ~10 dB of SNR on clean clips) without giving up denoising on noisy ones.                              |
| `tuning.postFilterBeta`     | `0.02`        | Valin post-filter strength applied after the mask/deep-filter stages; `0.0` disables it.                                                                                         | Larger suppresses residual "musical" noise more but can dull S/T/F consonants; `0.0` leaves the raw mask output.                                                                          |
| `tuning.attenuationLimitDb` | `100.0` (off) | Caps how far the residual noise may be attenuated below the input; the output is mixed back toward the input so the cap is never exceeded.                                       | Lower (e.g. `15`–`20`) keeps more of the original background but removes the hard gating/pumping that can sound "distorted"; `>= 100.0` disables the cap (full suppression, most gating). |

Engine-only knobs (`EnhancerOptions` in `enhancer_config.h`, not exposed to Kotlin):

* `min_db_thresh` (`-10` dB) — frames below this local SNR get a zero mask (silence). Raising it silences more.
* `max_db_erb_thresh` (`30` dB) — frames above this bypass enhancement entirely.
* `max_db_df_thresh` (`20` dB) — frames above this apply ERB gains only; others apply ERB gains + deep filtering.
* `enable_erb_stage` / `enable_df_stage` (`true`) — turn a stage off, for debugging.
* `intra_op_threads` / `inter_op_threads` (`1`), `use_nnapi` (`false`) — ONNX Runtime threading and NNAPI.

Adaptive-blend constants (`speech_enhancer_manager.cpp`): `kAdaptiveFullDb` (`5`),
`kAdaptiveFloorDb` (`20`), `kAdaptiveFloorStrength` (`0.15`), `kAdaptiveSpeechGateDb` (`15`) and
`kReferenceReleaseDbPerFrame` (`0.05`). They shape the SNR→blend curve and how fast the running
speech-reference estimate releases.

There is no setting that removes all background and also leaves the speech untouched: the
suppression character is a property of the model. Aggressive settings (attenuation limit off,
`strength = 1.0`) clean hard but gate/pump; gentle settings keep more noise.
