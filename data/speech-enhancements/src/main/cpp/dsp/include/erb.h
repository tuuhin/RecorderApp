#ifndef DFN_DSP_ERB_H_
#define DFN_DSP_ERB_H_

// ERB filterbank construction and band processing.
//
// Reference: libDF/src/lib.rs, `freq2erb`, `erb2freq`, `erb_fb`, `compute_band_corr`,
// `band_compr`, `apply_interp_band_gain`, `interp_band_gain`, `apply_band_gain`.
//
// The filterbank is rectangular: every frequency bin belongs to exactly one ERB band, and
// the per-band statistics are simple averages over that band's bins. This matches both the
// Rust implementation and the Python `df.modules.erb_fb` matrix with `normalized=True`.

#include <complex>
#include <vector>

namespace dfn::dsp {

// ERB scale used by DeepFilterNet: 9.265 * ln(1 + f / (24.7 * 9.265)).
float erb_from_hz(float freq_hz);
float hz_from_erb(float erb);

// Returns the number of frequency bins per ERB band. The widths always sum to
// fft_size / 2 + 1. `min_nb_freqs` enforces a lower bound per band, exactly like upstream.
std::vector<int> erb_band_widths(int sample_rate, int fft_size, int nb_bands, int min_nb_freqs = 2);

// Sum of all band widths (should equal fft_size / 2 + 1).
int erb_total_width(const std::vector<int>& widths);

// out[b] = (1 / |band_b|) * sum_{k in band_b} Re(x[k] * conj(p[k]))
void erb_band_correlation(const std::complex<float>* x, const std::complex<float>* p,
                          const std::vector<int>& widths, float* out);

// out[b] = (1 / |band_b|) * sum_{k in band_b} x[k]
void erb_band_compress(const float* x, const std::vector<int>& widths, float* out);

// Expands one gain per band onto every frequency bin.
void erb_interp_band_gain(float* out, const std::vector<int>& widths, const float* gains);

// Multiplies every frequency bin of the spectrum by the gain of its band.
void erb_apply_interp_band_gain(std::complex<float>* spectrum, const std::vector<int>& widths,
                                const float* gains);

}  // namespace dfn::dsp

#endif  // DFN_DSP_ERB_H_
