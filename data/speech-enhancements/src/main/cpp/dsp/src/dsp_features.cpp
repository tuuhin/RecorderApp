#include "dsp_features.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <ranges>
#include <span>

#include "erb.h"

namespace dfn::dsp {
namespace {

[[nodiscard]] float safe_sqrt_state(float state) noexcept {
    // The upstream Rust code divides by sqrt(state) without a guard. The state is initialized
    // with strictly positive values and only decays towards zero when alpha == 1, so a guard
    // is a no-op for all supported configurations but avoids a division by zero.
    return state > 0.0f ? std::sqrt(state) : 1.0f;
}

}  // namespace

void init_linear_state(std::span<float> state, float min, float max) {
    const auto n = state.size();
    if (n == 0) return;

    if (n == 1) {
        state[0] = min;
        return;
    }

    const auto denom = static_cast<float>(n - 1);
    for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, n)) {
        const float t = static_cast<float>(i) / denom;
        state[i] = std::lerp(min, max, t);
    }
}

void FeatureNormState::reset(int nb_erb, int nb_df) {
    const auto erb_count = static_cast<std::size_t>(std::max(nb_erb, 0));
    const auto df_count = static_cast<std::size_t>(std::max(nb_df, 0));
    erb_state.assign(erb_count, 0.0f);
    unit_state.assign(df_count, 0.0f);

    init_linear_state(erb_state, kMeanNormInitMin, kMeanNormInitMax);
    init_linear_state(unit_state, kUnitNormInitMin, kUnitNormInitMax);
}

void feat_erb(std::span<const std::complex<float>> spectrum, std::span<const int> widths,
              float alpha, std::span<float> state, std::span<float> out) {
    const std::size_t nb_erb = widths.size();
    const auto bins = static_cast<std::size_t>(erb_total_width(widths));
    const auto spec_span = spectrum.first(bins);
    const auto state_span = state.first(nb_erb);
    const auto out_span = out.first(nb_erb);

    // Band correlation of the spectrum with itself, i.e. mean power per band.
    erb_band_correlation(spec_span, spec_span, widths, out_span);

    for (const std::weakly_incrementable auto b : std::views::iota(std::size_t{0}, nb_erb)) {
        float db = std::log10(out_span[b] + kErbEnergyEpsilon) * 10.0f;
        // band_mean_norm_erb
        state_span[b] = db * (1.0f - alpha) + state_span[b] * alpha;
        db -= state_span[b];
        out_span[b] = db * kErbFeatureScale;
    }
}

void feat_cplx(std::span<const std::complex<float>> spectrum, std::size_t nb_df, float alpha,
               std::span<float> state, std::span<std::complex<float>> out) {
    const auto spec_span = spectrum.first(nb_df);
    const auto state_span = state.first(nb_df);
    const auto out_span = out.first(nb_df);

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{0}, nb_df)) {
        const std::complex<float> value = spec_span[k];
        const float magnitude = std::abs(value);
        state_span[k] = magnitude * (1.0f - alpha) + state_span[k] * alpha;
        out_span[k] = value / safe_sqrt_state(state_span[k]);
    }
}

void feat_cplx_transposed(std::span<const std::complex<float>> spectrum, std::size_t nb_df,
                          float alpha, std::span<float> state, std::span<float> out) {
    const auto spec_span = spectrum.first(nb_df);
    const auto state_span = state.first(nb_df);
    const auto out_span = out.first(2 * nb_df);
    auto out_re = out_span.first(nb_df);
    auto out_im = out_span.subspan(nb_df, nb_df);

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{0}, nb_df)) {
        const std::complex<float> value = spec_span[k];
        const float magnitude = std::abs(value);
        state_span[k] = magnitude * (1.0f - alpha) + state_span[k] * alpha;
        const float inv = 1.0f / safe_sqrt_state(state_span[k]);
        out_re[k] = value.real() * inv;
        out_im[k] = value.imag() * inv;
    }
}

void feat_erb_sequence(std::span<const std::complex<float>> spectrogram, std::size_t frame_count,
                       std::span<const int> widths, float alpha, std::span<float> state,
                       std::span<float> out) {
    const auto bins = static_cast<std::size_t>(erb_total_width(widths));
    const std::size_t nb_erb = widths.size();
    const auto spec_span = spectrogram.first(frame_count * bins);
    const auto out_span = out.first(frame_count * nb_erb);

    for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, frame_count)) {
        feat_erb(spec_span.subspan(f * bins, bins),
                 widths,
                 alpha,
                 state,
                 out_span.subspan(f * nb_erb, nb_erb));
    }
}

}  // namespace dfn::dsp
