# onnx-tooling

Host-side Python tooling for preparing the DeepFilterNet 3 ONNX graphs used by the
speech-enhancement engine. **Not** part of the Android build — it runs on a developer
machine (or CI), never on the device.

## Why conversion is needed

The graphs published upstream (`DeepFilterNet3_onnx.tar.gz`, mirrored on Hugging Face at
[`bitsydarel/deepfilternet3-onnx`](https://huggingface.co/bitsydarel/deepfilternet3-onnx))
are **batch** exports. They cannot be driven frame by frame:

- their `GRU` nodes start from a baked-in zero hidden state on every `Run()`, so the
  recurrent state cannot be carried across hops; and
- their causal convolution front-ends zero-pad the time axis themselves, which replaces
  the real history of every frame with zeros when a single frame is fed.

`prepare_models.py` performs two deterministic edits and verifies the result is bit-exact
against the original batch graph:

1. every `GRU` node gets a new `state_in_<i>` input (replacing `initial_h`) and exposes its
   `Y_h` as `state_out_<i>`;
2. the time-axis `Pad` of the encoder input convolutions is disabled and a `Slice` keeps
   only the last time step, so the caller (the C++ engine) supplies real history frames (3-frame window for `enc`,
   5-frame for `df_dec`).

The C++ engine (`src/main/cpp/enhancer_core/src/onnx_backend.cpp`) rejects a graph without
`state_in_*` inputs, so the raw downloads must be converted before use.

## Files

| File                             | Role                                                                                                                                                                                                        |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prepare_models.py`              | batch → streaming graph conversion (+ equivalence check), needed by the tests / app                                                                                                                         |
| `reference_streaming.py`         | Python re-implementation of the streaming pipeline; the executable spec for the C++ engine and the source of the C++ reference vectors (needs upstream `libdf`, so it is not covered by `requirements.txt`) |
| `generate_reference.py`          | regenerates the C++ DSP unit-test reference vectors from the official `libdf` (needs `deepfilternet==0.5.6`)                                                                                                |
| `generate_enhancer_reference.py` | regenerates the end-to-end enhancer/ONNX reference vectors via `reference_streaming.py` (needs `libdf` + the prepared streaming graphs)                                                                     |
| `requirements.txt`               | pinned deps for `prepare_models.py` (`numpy`, `onnx`, `onnxruntime`)                                                                                                                                        |

## Running it

The normal path is the module-level asset script, which downloads the raw graphs, builds a
throwaway `uv` environment, converts, and drops the prepared graphs into the Android test
assets:

```bash
./build_tests_assets.sh          # add --force to re-download / re-convert
```

Manually:

```bash
uv venv .venv --python 3.14 --allow-existing
uv pip install --python .venv/bin/python -r requirements.txt
.venv/bin/python prepare_models.py --model-dir <raw graphs> --out-dir <out>
```

`prepare_models.py` emits `enc_stream.onnx`, `erb_dec_stream.onnx`, `df_dec_stream.onnx`;
the engine expects them renamed to `enc.onnx`, `erb_dec.onnx`, `df_dec.onnx` next to
`config.ini`.

## Reference-vector generators

`generate_reference.py` and `generate_enhancer_reference.py` regenerate the raw float32
vectors the C++ unit tests compare against. They are not needed to build the Android test
assets, need the extra `deepfilternet==0.5.6` (`libdf`) dependency, and are run by hand
against the prepared streaming graphs:

```bash
python generate_reference.py                                   # → ../../src/main/cpp/dsp/test/data
python generate_enhancer_reference.py --model-dir <streaming graphs> --out <dir>
```

## Producer-side only

This tooling never ships to the device. When the model is downloaded at runtime instead of
bundled, the converted graphs are published once (HF repo / bucket / release asset) with
their SHA-256 checksums, and the app downloads the *already-converted* files — the same
pattern `data:transcribe` uses for its Vosk models (`model_uri` + `checksum_sha256`).
