#include "dsp_features.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>

#include "erb.h"

namespace dfn::dsp {
namespace {

inline float safe_sqrt_state(float state) {
    // The upstream Rust code divides by sqrt(state) without a guard. The state is initialized
    // with strictly positive values and only decays towards zero when alpha == 1, so a guard
    // is a no-op for all supported configurations but avoids a division by zero.
    return state > 0.0f ? std::sqrt(state) : 1.0f;
}

}  // namespace

void init_linear_state(std::vector<float>& state, float min, float max) {
    const auto n = static_cast<int>(state.size());
    if (n <= 0) {
        return;
    }
    if (n == 1) {
        state[0] = min;
        return;
    }
    const float step = (max - min) / static_cast<float>(n - 1);
    for (int i = 0; i < n; ++i) {
        state[static_cast<std::size_t>(i)] = min + static_cast<float>(i) * step;
    }
}

void FeatureNormState::reset(int nb_erb, int nb_df) {
    erb_state.assign(static_cast<std::size_t>(std::max(nb_erb, 0)), 0.0f);
    unit_state.assign(static_cast<std::size_t>(std::max(nb_df, 0)), 0.0f);
    init_linear_state(erb_state, kMeanNormInitMin, kMeanNormInitMax);
    init_linear_state(unit_state, kUnitNormInitMin, kUnitNormInitMax);
}

void feat_erb(const std::complex<float>* spectrum, const std::vector<int>& widths, float alpha,
              float* state, float* out) {
    // Band correlation of the spectrum with itself, i.e. mean power per band.
    erb_band_correlation(spectrum, spectrum, widths, out);

    const std::size_t nb_erb = widths.size();
    for (std::size_t b = 0; b < nb_erb; ++b) {
        float value = out[b];
        value = std::log10(value + kErbEnergyEpsilon) * 10.0f;
        // band_mean_norm_erb
        state[b] = value * (1.0f - alpha) + state[b] * alpha;
        value -= state[b];
        out[b] = value * kErbFeatureScale;
    }
}

void feat_cplx(const std::complex<float>* spectrum, int nb_df, float alpha, float* state,
               std::complex<float>* out) {
    for (int k = 0; k < nb_df; ++k) {
        const std::complex<float> value = spectrum[k];
        const float magnitude = std::abs(value);
        state[k] = magnitude * (1.0f - alpha) + state[k] * alpha;
        out[k] = value / safe_sqrt_state(state[k]);
    }
}

void feat_cplx_transposed(const std::complex<float>* spectrum, int nb_df, float alpha, float* state,
                          float* out) {
    float* out_re = out;
    float* out_im = out + nb_df;
    for (int k = 0; k < nb_df; ++k) {
        const std::complex<float> value = spectrum[k];
        const float magnitude = std::abs(value);
        state[k] = magnitude * (1.0f - alpha) + state[k] * alpha;
        const float inv = 1.0f / safe_sqrt_state(state[k]);
        out_re[k] = value.real() * inv;
        out_im[k] = value.imag() * inv;
    }
}

void feat_erb_sequence(const std::complex<float>* spectrogram, int frame_count,
                       const std::vector<int>& widths, float alpha, float* state, float* out) {
    const auto bins = static_cast<std::size_t>(erb_total_width(widths));
    const std::size_t nb_erb = widths.size();
    for (int f = 0; f < frame_count; ++f) {
        feat_erb(spectrogram + static_cast<std::size_t>(f) * bins,
                 widths,
                 alpha,
                 state,
                 out + static_cast<std::size_t>(f) * nb_erb);
    }
}

}  // namespace dfn::dsp
