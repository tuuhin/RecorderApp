#!/usr/bin/env python3
"""Streaming reference implementation of the DeepFilterNet 3 pipeline.

This script is the executable specification for the C++ engine in
`src/main/cpp/enhancer`. It mirrors the *streaming* semantics of the upstream Rust
implementation (libDF/src/lib.rs for the DSP and libDF/src/tract.rs for the model
orchestration) using:

  * `libdf` for STFT/ISTFT, the ERB band layout and the reference feature normalization,
  * the official ONNX graphs (`enc.onnx`, `erb_dec.onnx`, `df_dec.onnx`), prepared once by
    `prepare_models.py` into per-frame ("stream") graphs with explicit GRU state inputs.

It is used to (a) confirm the streaming pipeline is equivalent to the offline Python
`enhance()`, and (b) produce reference vectors for the C++ unit tests.

Run:
    python onnx-tooling/reference_streaming.py --model-dir <dir with *_stream.onnx> \
        [--input audio.wav] [--output reference.npz]

This module additionally needs `libdf` (the upstream Rust extension), so it is not
covered by `requirements.txt`; that file only pins the dependencies needed by
`prepare_models.py` for the test-asset build.
"""

import argparse
import os

import numpy as np


class StreamingEnhancer:
    """Frame-by-frame DeepFilterNet 3 pipeline (single channel, float32)."""

    def __init__(self, model_dir, post_filter_beta=0.0, atten_lim_db=100.0, threads=1):
        import onnxruntime as ort

        self.ort = ort
        self.sr = 48000
        self.fft_size = 960
        self.hop = 480
        self.nb_erb = 32
        self.nb_df = 96
        self.df_order = 5
        self.df_lookahead = 2
        self.conv_lookahead = 2
        self.alpha = 0.99
        self.lookahead = max(self.df_lookahead, self.conv_lookahead)

        opts = ort.SessionOptions()
        opts.intra_op_num_threads = threads
        opts.inter_op_num_threads = threads
        self.enc = ort.InferenceSession(os.path.join(model_dir, "enc_stream.onnx"), opts,
                                        providers=["CPUExecutionProvider"])
        self.erb_dec = ort.InferenceSession(os.path.join(model_dir, "erb_dec_stream.onnx"), opts,
                                           providers=["CPUExecutionProvider"])
        self.df_dec = ort.InferenceSession(os.path.join(model_dir, "df_dec_stream.onnx"), opts,
                                          providers=["CPUExecutionProvider"])
        self.enc_out = [o.name for o in self.enc.get_outputs()]
        self.erb_out = [o.name for o in self.erb_dec.get_outputs()]
        self.df_out = [o.name for o in self.df_dec.get_outputs()]

        self.post_filter_beta = post_filter_beta
        self.atten_lim = None if abs(atten_lim_db) >= 100 else 10 ** (-abs(atten_lim_db) / 20.0)
        self.reset()

    def reset(self):
        z3 = lambda: np.zeros((1, 1, 256), np.float32)
        self.mean_state = np.linspace(-60.0, -90.0, self.nb_erb).astype(np.float32)
        self.unit_state = np.linspace(0.001, 0.0001, self.nb_df).astype(np.float32)
        self.enc_state = z3()
        self.erb_state = [z3(), z3()]
        self.df_state = [z3(), z3()]
        # encoder input history: 2 previous frames
        self.erb_hist = np.zeros((1, 1, 3, self.nb_erb), np.float32)
        self.spec_hist = np.zeros((1, 2, 3, self.nb_df), np.float32)
        # decoder input rings
        self.emb_hist = np.zeros((1, 5, 512), np.float32)
        self.c0_hist = np.zeros((1, 64, 5, self.nb_df), np.float32)
        # spectrogram rings
        self.roll_y = [np.zeros(481, np.complex64) for _ in range(self.df_order + self.conv_lookahead)]
        self.roll_x = [np.zeros(481, np.complex64) for _ in range(max(self.df_order, self.lookahead))]
        self.last_mask = np.zeros(self.nb_erb, np.float32)
        self.last_coefs = np.zeros((self.nb_df, self.df_order, 2), np.float32)
        self.last_encoder = {}
        self._df = None

    # ------------------------------------------------------------------ DSP helpers
    def analysis(self, x):
        import libdf
        if self._df is None:
            self._df = libdf.DF(sr=self.sr, fft_size=self.fft_size, hop_size=self.hop,
                                nb_bands=self.nb_erb, min_nb_erb_freqs=2)
        return np.asarray(self._df.analysis(x.reshape(1, -1).astype(np.float32)))[0]

    # ------------------------------------------------------------------ features
    def _erb_feature(self, spec_frame):
        p = np.abs(spec_frame) ** 2
        bands = np.array([p[b:b + w].mean() for b, w in self._band_ranges()], np.float32)
        db = np.log10(bands + 1e-10) * 10.0
        self.mean_state = db * (1 - self.alpha) + self.mean_state * self.alpha
        return (db - self.mean_state) / 40.0

    def _band_ranges(self):
        import libdf
        if self._df is None:
            self._df = libdf.DF(sr=self.sr, fft_size=self.fft_size, hop_size=self.hop,
                                nb_bands=self.nb_erb, min_nb_erb_freqs=2)
        widths = np.asarray(self._df.erb_widths()).astype(np.int64)
        bounds = np.concatenate([[0], np.cumsum(widths)])
        return [(bounds[i], widths[i]) for i in range(len(widths))]

    def _cplx_feature(self, spec_frame):
        xs = spec_frame[: self.nb_df]
        self.unit_state = np.abs(xs) * (1 - self.alpha) + self.unit_state * self.alpha
        return xs / np.sqrt(self.unit_state)

    # ------------------------------------------------------------------ model
    @staticmethod
    def _pick(outputs, *names):
        """Returns the first present output; tolerates the `*_lastframe` renames that the
        prepared streaming graphs use for the encoder taps."""
        for name in names:
            if name in outputs:
                return np.asarray(outputs[name])
        raise KeyError(f"none of {names} in model outputs {sorted(outputs)}")

    def _run(self, spec_frame):
        erb = self._erb_feature(spec_frame)
        cplx = self._cplx_feature(spec_frame)

        self.erb_hist[0, 0, 0:2] = self.erb_hist[0, 0, 1:3]
        self.erb_hist[0, 0, 2] = erb
        self.spec_hist[0, :, 0:2, :] = self.spec_hist[0, :, 1:3, :]
        self.spec_hist[0, 0, 2, :] = cplx.real
        self.spec_hist[0, 1, 2, :] = cplx.imag

        enc = dict(zip(self.enc_out, self.enc.run(None, {
            "feat_erb": self.erb_hist, "feat_spec": self.spec_hist,
            "state_in_0": self.enc_state})))
        self.enc_state = enc["state_out_0"]
        emb = np.asarray(enc["emb"])          # [1,1,512]
        c0 = self._pick(enc, "c0", "c0_lastframe")   # [1,64,1,96]
        e0 = self._pick(enc, "e0", "e0_lastframe")
        e1 = np.asarray(enc["e1"])
        e2, e3 = np.asarray(enc["e2"]), np.asarray(enc["e3"])
        lsnr = float(np.asarray(enc["lsnr"]).reshape(-1)[0])

        self.emb_hist[0, 0:4] = self.emb_hist[0, 1:5]
        self.emb_hist[0, 4] = emb[0, 0]
        self.c0_hist[0, :, 0:4, :] = self.c0_hist[0, :, 1:5, :]
        self.c0_hist[0, :, 4, :] = c0[0, :, 0, :]

        erb_out = dict(zip(self.erb_out, self.erb_dec.run(None, {
            "emb": emb, "e3": e3, "e2": e2, "e1": e1, "e0": e0,
            "state_in_0": self.erb_state[0], "state_in_1": self.erb_state[1]})))
        self.erb_state = [erb_out["state_out_0"], erb_out["state_out_1"]]
        mask = np.asarray(erb_out["m"]).reshape(-1)

        df_out = dict(zip(self.df_out, self.df_dec.run(None, {
            "emb": self.emb_hist, "c0": self.c0_hist,
            "state_in_0": self.df_state[0], "state_in_1": self.df_state[1]})))
        self.df_state = [df_out["state_out_0"], df_out["state_out_1"]]
        coefs = np.asarray(df_out["coefs"]).reshape(self.nb_df, self.df_order, 2)
        self.last_mask = mask
        self.last_coefs = coefs
        self.last_encoder = enc
        return lsnr, mask, coefs

    @staticmethod
    def _stages(lsnr):
        if lsnr < -10.0:
            return False, True, False
        if lsnr > 30.0:
            return False, False, False
        if lsnr > 20.0:
            return True, False, False
        return True, False, True

    # ------------------------------------------------------------------ frame step
    def process_frame(self, spec_frame, widths):
        """Consumes one noisy spectrum, returns (enhanced spectrum for the delayed
        frame, lsnr). The returned spectrum corresponds to input frame t - 2."""
        self.roll_y.pop(0)
        self.roll_y.append(spec_frame)
        self.roll_x.pop(0)
        self.roll_x.append(spec_frame)

        lsnr, mask, coefs = self._run(spec_frame)
        apply_erb, apply_zeros, apply_df = self._stages(lsnr)

        index = self.df_order - 1  # t - lookahead
        y = self.roll_y[index].copy()
        if apply_erb:
            y = y * np.repeat(mask, widths)
        elif apply_zeros:
            y = np.zeros_like(y)
        if apply_df:
            acc = np.zeros(self.nb_df, np.complex64)
            for k in range(self.df_order):
                acc += self.roll_x[k][: self.nb_df] * (
                    coefs[:, k, 0] + 1j * coefs[:, k, 1])
            y[: self.nb_df] = acc
        if apply_erb and self.post_filter_beta > 0:
            y = self._post_filter(self.roll_x[self.lookahead], y)
        if self.atten_lim is not None:
            y = y * (1.0 - self.atten_lim) + self.roll_x[self.lookahead] * self.atten_lim
        return y, lsnr

    def _post_filter(self, noisy, enh):
        eps = 1e-12
        g = np.clip(np.abs(enh) / (np.abs(noisy) + eps), eps, 1.0)
        g_sin = g * np.sin(np.pi * g / 2.0) + eps
        beta = self.post_filter_beta
        pf = (1 + beta) / (1 + beta * (g / g_sin) ** 2)
        return enh * pf

    # ------------------------------------------------------------------ whole signal
    def enhance(self, signal, widths):
        """Offline convenience: returns the enhanced spectrum per output frame and the
        per-frame lsnr. Output frame k corresponds to input frame k (caller compensates
        the algorithmic delay)."""
        spec = self.analysis(signal)
        frames = spec.shape[0]
        out = np.zeros_like(spec)
        lsnrs = np.zeros(frames, np.float32)
        for t in range(frames):
            y, lsnr = self.process_frame(spec[t], widths)
            lsnrs[t] = lsnr
            k = t - self.lookahead
            if k >= 0:
                out[k] = y
        return out, lsnrs, spec


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--input", default=None)
    parser.add_argument("--output", default=None)
    args = parser.parse_args()

    import libdf
    if args.input:
        import soundfile as sf
        data, sr = sf.read(args.input, dtype="float32", always_2d=True)
        mono = data.mean(axis=1)
        if sr != 48000:
            raise SystemExit(f"expected 48 kHz input, got {sr}")
        signal = mono.astype(np.float32)
    else:
        n = 48000
        t = np.arange(n, dtype=np.float64)
        rng = np.random.RandomState(3)
        signal = (0.25 * np.sin(2 * np.pi * 440 * t / 48000)
                  + 0.10 * np.sin(2 * np.pi * 1700 * t / 48000)
                  + 0.02 * rng.standard_normal(n)).astype(np.float32)

    enhancer = StreamingEnhancer(args.model_dir)
    df = libdf.DF(sr=48000, fft_size=960, hop_size=480, nb_bands=32, min_nb_erb_freqs=2)
    widths = np.asarray(df.erb_widths()).astype(np.int64)
    spectrum, lsnrs, spec = enhancer.enhance(signal, widths)
    audio = np.asarray(
        df.synthesis(spectrum.reshape(1, spectrum.shape[0], -1).astype(np.complex64))).reshape(-1)
    print(f"frames={spec.shape[0]} audio={audio.shape} lsnr=[{lsnrs.min():.2f},{lsnrs.max():.2f}]")
    if args.output:
        np.savez(args.output, audio=audio, spectrum=spectrum, lsnr=lsnrs,
                 spec=spec, signal=signal)
        print("wrote", args.output)


if __name__ == "__main__":
    main()