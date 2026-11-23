#include "deep_filter.h"

#include <algorithm>
#include <cmath>

namespace dfn::dsp {

void apply_deep_filter(const std::complex<float>* history, int df_order,
                       const std::complex<float>* coefs, int nb_df, int n_freqs,
                       std::complex<float>* spectrum) {
    if (history == nullptr || coefs == nullptr || spectrum == nullptr) {
        return;
    }
    const int bins = std::min(nb_df, n_freqs);
    // Zero the relevant bins, then accumulate every filter tap.
    for (int f = 0; f < bins; ++f) {
        spectrum[f] = std::complex<float>(0.0f, 0.0f);
    }
    for (int k = 0; k < df_order; ++k) {
        const std::complex<float>* frame = history + static_cast<std::size_t>(k) * n_freqs;
        const std::complex<float>* coef_frame = coefs + static_cast<std::size_t>(k) * nb_df;
        for (int f = 0; f < bins; ++f) {
            spectrum[f] += frame[f] * coef_frame[f];
        }
    }
}

void apply_post_filter(const std::complex<float>* noisy, std::complex<float>* enhanced, int count,
                       float beta) {
    const float beta_p1 = beta + 1.0f;
    const float eps = 1e-12f;
    const float pi = 3.14159265358979323846f;
    for (int i = 0; i < count; ++i) {
        const float num = std::abs(enhanced[i]);
        const float den = std::abs(noisy[i]) + eps;
        const float g = std::min(std::max(num / den, eps), 1.0f);
        const float g_sin = g * std::sin(g * pi / 2.0f);
        const float ratio = g / g_sin;
        const float pf = (beta_p1 * g / (1.0f + beta * ratio * ratio)) / g;
        enhanced[i] *= pf;
    }
}

void apply_attenuation_limit(const std::complex<float>* noisy, std::complex<float>* enhanced,
                             int count, float alpha) {
    const float blend = 1.0f - alpha;
    for (int i = 0; i < count; ++i) {
        enhanced[i] = enhanced[i] * blend + noisy[i] * alpha;
    }
}

}  // namespace dfn::dsp
