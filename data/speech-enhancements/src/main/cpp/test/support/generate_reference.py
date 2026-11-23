#!/usr/bin/env python3
"""Generates the numerical reference vectors used by the C++ DSP unit tests.

The reference values come from the *official* DeepFilterNet libraries:

  * `libdf` (the Rust DSP shipped with the Python package `deepfilternet==0.5.6`)
    provides the STFT/ISTFT, the ERB band layout and the ERB/unit normalization.
  * The ONNX graphs are the ones shipped in `models/DeepFilterNet3_onnx.tar.gz`
    (config.ini confirms sr=48000, fft_size=960, hop_size=480, nb_erb=32, nb_df=96).

Usage:
    python test/support/generate_reference.py [--out dsp/test/data]

Requirements: numpy, deepfilternet==0.5.6 (installs the `libdf` extension).

Arrays are written as raw little-endian float32 files; complex data is stored as
interleaved (re, im) float32. Each array has a companion `.shape` text file.
"""

import argparse
import os

import numpy as np


def save(path: str, name: str, array) -> None:
    a = np.asarray(array)
    if np.iscomplexobj(a):
        a = np.ascontiguousarray(a).view(np.float32)
    else:
        a = np.ascontiguousarray(a, dtype=np.float32)
    a.reshape(-1).tofile(os.path.join(path, name + ".f32"))
    with open(os.path.join(path, name + ".shape"), "w", encoding="utf-8") as handle:
        handle.write(" ".join(str(d) for d in np.shape(array)))
    print(f"{name}: shape={np.shape(array)} dtype={np.asarray(array).dtype}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "..", "..", "dsp", "test", "data"))
    args = parser.parse_args()
    out = os.path.abspath(args.out)
    os.makedirs(out, exist_ok=True)

    import libdf

    sr, fft_size, hop_size, nb_erb, nb_df = 48000, 960, 480, 32, 96
    df = libdf.DF(sr=sr, fft_size=fft_size, hop_size=hop_size, nb_bands=nb_erb,
                  min_nb_erb_freqs=2)

    # --- Window -------------------------------------------------------------------
    save(out, "window_960", np.asarray(df.fft_window(), dtype=np.float32))

    # --- Deterministic input signal (440 Hz + 1 kHz + seeded noise) ----------------
    n = np.arange(2880, dtype=np.float64)
    rng = np.random.RandomState(1234)
    signal = (
        0.4 * np.sin(2.0 * np.pi * 440.0 * n / sr)
        + 0.25 * np.sin(2.0 * np.pi * 1000.0 * n / sr)
        + 0.05 * rng.standard_normal(n.size)
    ).astype(np.float32)
    save(out, "stft_signal", signal)

    # --- STFT (streaming analysis of the whole signal) ----------------------------
    spectrum = np.asarray(df.analysis(signal.reshape(1, -1)))  # [1, frames, 481] complex64
    save(out, "stft_spec", spectrum[0])

    # --- ISTFT (synthesis of exactly that spectrum) -------------------------------
    reconstructed = np.asarray(df.synthesis(spectrum.astype(np.complex64)))
    save(out, "istft_out", reconstructed.reshape(-1))

    # --- ERB filterbank layout ----------------------------------------------------
    widths = np.asarray(df.erb_widths(), dtype=np.float32)
    save(out, "erb_widths", widths)

    # --- ERB and complex features (first 4 frames, alpha = 0.99) ------------------
    alpha = 0.99
    frames = 4
    spectrogram = spectrum[0, :frames]  # [frames, 481] complex64
    erb_raw = np.asarray(libdf.erb(spectrogram.reshape(1, frames, -1), df.erb_widths()))
    erb_norm = np.asarray(libdf.erb_norm(erb_raw, alpha))
    save(out, "erb_feat_input", spectrogram)
    save(out, "erb_feat", erb_norm[0])

    unit_norm = np.asarray(libdf.unit_norm(spectrogram.reshape(1, frames, -1)[..., :nb_df], alpha))
    save(out, "unit_norm", unit_norm[0])


if __name__ == "__main__":
    main()