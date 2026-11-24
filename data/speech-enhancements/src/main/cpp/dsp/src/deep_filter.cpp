#include "deep_filter.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <numbers>
#include <ranges>
#include <span>

namespace dfn::dsp {

void apply_deep_filter(std::span<const std::complex<float>> history, std::size_t df_order,
                       std::span<const std::complex<float>> coefs, std::size_t nb_df,
                       std::size_t n_freqs, std::span<std::complex<float>> spectrum) {
    const std::size_t bins = std::min(nb_df, n_freqs);
    if (bins == 0 || df_order == 0) return;

    const auto hist_span = history.first(df_order * n_freqs);
    const auto coef_span = coefs.first(df_order * nb_df);
    auto spec_span = spectrum.first(n_freqs);
    auto active_bins = spec_span.first(bins);

    // Zero the relevant bins, then accumulate every filter tap.
    std::ranges::fill(active_bins, std::complex<float>{0.0f, 0.0f});

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{0}, df_order)) {
        const auto frame = hist_span.subspan(k * n_freqs, bins);
        const auto coef_frame = coef_span.subspan(k * nb_df, bins);
        for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, bins)) {
            active_bins[f] += frame[f] * coef_frame[f];
        }
    }
}

void apply_post_filter(std::span<const std::complex<float>> noisy,
                       std::span<std::complex<float>> enhanced, std::size_t count, float beta) {
    constexpr float kEps = 1e-12f;
    constexpr float kHalfPi = 0.5f * std::numbers::pi_v<float>;
    const float beta_p1 = beta + 1.0f;

    const auto noisy_span = noisy.first(count);
    auto enhanced_span = enhanced.first(count);

    for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, count)) {
        const float num = std::abs(enhanced_span[i]);
        const float den = std::abs(noisy_span[i]) + kEps;
        const float g = std::clamp(num / den, kEps, 1.0f);
        const float g_sin = g * std::sin(g * kHalfPi);
        const float ratio = g / g_sin;
        const float pf = (beta_p1 * g / (1.0f + beta * ratio * ratio)) / g;
        enhanced_span[i] *= pf;
    }
}

void apply_attenuation_limit(std::span<const std::complex<float>> noisy,
                             std::span<std::complex<float>> enhanced, std::size_t count,
                             float alpha) {
    const auto noisy_span = noisy.first(count);
    auto enhanced_span = enhanced.first(count);

    for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, count)) {
        // std::lerp(a, b, t) computes a + t * (b - a), matching (1 - alpha) * enhanced + alpha *
        // noisy
        const float re = std::lerp(enhanced_span[i].real(), noisy_span[i].real(), alpha);
        const float im = std::lerp(enhanced_span[i].imag(), noisy_span[i].imag(), alpha);
        enhanced_span[i] = {re, im};
    }
}

}  // namespace dfn::dsp
