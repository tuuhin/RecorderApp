#!/usr/bin/env python3
"""Generates end-to-end reference vectors for the enhancer / ONNX tests.

Runs the streaming reference pipeline (tools/reference_streaming.py) on a deterministic
speech-like signal and stores every intermediate the C++ tests compare against.

The model files are not committed to the repository, so this script needs the prepared
streaming graphs (tools/prepare_models.py). Its output is therefore written outside the
source tree by default; point the tests at it with -DDFN_TEST_ENHANCER_DATA=<dir>.

Usage:
    python test/support/generate_enhancer_reference.py \
        --model-dir <dir with *_stream.onnx> --out <dir>
"""

import argparse
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.abspath(os.path.join(HERE, "..", "..", "tools")))

from reference_streaming import StreamingEnhancer  # noqa: E402


def save(path: str, name: str, array) -> None:
    a = np.asarray(array)
    if np.iscomplexobj(a):
        a = np.ascontiguousarray(a).view(np.float32)
    else:
        a = np.ascontiguousarray(a, dtype=np.float32)
    a.reshape(-1).tofile(os.path.join(path, name + ".f32"))
    with open(os.path.join(path, name + ".shape"), "w", encoding="utf-8") as handle:
        handle.write(" ".join(str(d) for d in np.shape(array)))
    print(f"{name}: {np.shape(array)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--seconds", type=float, default=0.5)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)

    import libdf

    sr = 48000
    n = int(sr * args.seconds)
    t = np.arange(n, dtype=np.float64)
    rng = np.random.RandomState(17)
    # Speech-like signal: voiced tone with a slow amplitude envelope plus noise.
    envelope = 0.5 + 0.5 * np.sin(2.0 * np.pi * 2.5 * t / sr)
    signal = (0.2 * envelope * np.sin(2.0 * np.pi * 220.0 * t / sr)
              + 0.05 * envelope * np.sin(2.0 * np.pi * 660.0 * t / sr)
              + 0.01 * rng.standard_normal(n)).astype(np.float32)
    save(args.out, "signal", signal)

    df = libdf.DF(sr=sr, fft_size=960, hop_size=480, nb_bands=32, min_nb_erb_freqs=2)
    widths = np.asarray(df.erb_widths()).astype(np.int64)
    enhancer = StreamingEnhancer(args.model_dir)

    # Capture encoder outputs for the first frames (backend-level assertions).
    spectrum, lsnr, spec = enhancer.enhance(signal, widths)
    audio = np.asarray(df.synthesis(spectrum.reshape(1, spectrum.shape[0], -1)
                                    .astype(np.complex64))).reshape(-1)
    save(args.out, "audio", audio)
    save(args.out, "lsnr", lsnr)
    save(args.out, "spec_enhanced", spectrum)
    save(args.out, "noisy_spec", spec)

    # Re-run a few frames to capture the ERB mask and DF coefficients.
    enhancer.reset()
    frames = spec.shape[0]
    masks = np.zeros((frames, 32), np.float32)
    coefs = np.zeros((frames, 96, 5, 2), np.float32)
    for index in range(frames):
        frame, _ = enhancer.process_frame(spec[index], widths)
        masks[index] = enhancer.last_mask
        coefs[index] = enhancer.last_coefs.reshape(96, 5, 2)
    save(args.out, "mask", masks)
    save(args.out, "coefs", coefs)
    print(f"frames={frames}")


if __name__ == "__main__":
    main()