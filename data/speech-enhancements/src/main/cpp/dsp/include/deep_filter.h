#ifndef DFN_DSP_DEEP_FILTER_H_
#define DFN_DSP_DEEP_FILTER_H_

// Deep filtering and the optional post-filter.
//
// Reference: libDF/src/tract.rs, `df()`, and libDF/src/lib.rs, `post_filter`.
//
// Deep filtering replaces the low-frequency bins of the stage-1 enhanced spectrum with a
// multi-frame complex linear combination:
//     out[t, f] = sum_{k=0}^{df_order-1} coefs[t, k, f] * spec_history[k, f]
// where `spec_history` holds consecutive spectra (oldest first) and only the first
// `nb_df` frequency bins are touched.

#include <complex>
#include <vector>

namespace dfn::dsp {

// Applies deep filtering in place to `spectrum`.
//
// history: `df_order * n_freqs` interleaved complex values, frame-major, oldest first.
// coefs:   `df_order * nb_df` interleaved complex values, `coefs[k * nb_df + f]`.
// spectrum: `n_freqs` bins, modified in place (bins [0, nb_df) are replaced, the rest is
//           preserved).
void apply_deep_filter(const std::complex<float>* history, int df_order,
                       const std::complex<float>* coefs, int nb_df, int n_freqs,
                       std::complex<float>* spectrum);

// Valin et al. post-filter, as used by DeepFilterNet. `enhanced` is modified in place.
void apply_post_filter(const std::complex<float>* noisy, std::complex<float>* enhanced, int count,
                       float beta);

// Multiplies `enhanced` by the attenuation limit mask for a per-frame `alpha` in [0, 1]:
//     out = (1 - alpha) * enhanced + alpha * noisy
void apply_attenuation_limit(const std::complex<float>* noisy, std::complex<float>* enhanced,
                             int count, float alpha);

}  // namespace dfn::dsp

#endif  // DFN_DSP_DEEP_FILTER_H_