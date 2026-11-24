#include "erb.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <numeric>
#include <ranges>
#include <span>
#include <stdexcept>

namespace dfn::dsp {

float erb_from_hz(float freq_hz) {
    return 9.265f * std::log1p(freq_hz / (24.7f * 9.265f));
}

float hz_from_erb(float erb) {
    return 24.7f * 9.265f * std::expm1(erb / 9.265f);
}

std::vector<int> erb_band_widths(int sample_rate, int fft_size, int nb_bands, int min_nb_freqs) {
    if (sample_rate <= 0 || fft_size <= 0 || nb_bands <= 0)
        throw std::invalid_argument(
            "erb_band_widths requires positive sample rate, fft size, bands");

    if (min_nb_freqs <= 0) throw std::invalid_argument("erb_band_widths requires min_nb_freqs > 0");

    const int nyq_freq = sample_rate / 2;
    const auto freq_width = static_cast<float>(sample_rate) / static_cast<float>(fft_size);
    const float erb_low = erb_from_hz(0.0f);
    const float erb_high = erb_from_hz(static_cast<float>(nyq_freq));
    const float step = (erb_high - erb_low) / static_cast<float>(nb_bands);

    std::vector<int> widths(static_cast<std::size_t>(nb_bands), 0);
    int prev_freq = 0;  // last frequency bin of the previous band
    int freq_over = 0;  // bins already forced into previous bands

    for (const int i : std::views::iota(1, nb_bands + 1)) {
        const float f = hz_from_erb(erb_low + static_cast<float>(i) * step);
        const auto fb = static_cast<int>(std::round(f / freq_width));
        int nb_freqs = fb - prev_freq - freq_over;
        if (nb_freqs < min_nb_freqs) {
            freq_over = min_nb_freqs - nb_freqs;
            nb_freqs = min_nb_freqs;
        } else {
            freq_over = 0;
        }
        widths[static_cast<std::size_t>(i - 1)] = nb_freqs;
        prev_freq = fb;
    }

    // There are fft_size / 2 + 1 frequency bins, one more than the ERB bands cover.
    widths.back() += 1;

    const int total = erb_total_width(widths);
    const int too_large = total - (fft_size / 2 + 1);
    if (too_large > 0) widths.back() -= too_large;
    return widths;
}

int erb_total_width(std::span<const int> widths) {
    return std::accumulate(widths.begin(), widths.end(), 0);
}

void erb_band_correlation(std::span<const std::complex<float>> x,
                          std::span<const std::complex<float>> p, std::span<const int> widths,
                          std::span<float> out) {
    std::size_t offset = 0;
    for (const auto b : std::views::iota(std::size_t{0}, widths.size())) {
        const int width = widths[b];
        if (width <= 0) {
            out[b] = 0.0f;
        } else {
            const auto band_width = static_cast<std::size_t>(width);
            const float scale = 1.0f / static_cast<float>(width);
            const auto x_band = x.subspan(offset, band_width);
            const auto p_band = p.subspan(offset, band_width);

            float acc = 0.0f;
            for (const auto j : std::views::iota(std::size_t{0}, band_width)) {
                acc += (x_band[j].real() * p_band[j].real() + x_band[j].imag() * p_band[j].imag()) *
                       scale;
            }
            out[b] = acc;
        }
        offset += static_cast<std::size_t>(width);
    }
}

void erb_band_compress(std::span<const float> x, std::span<const int> widths,
                       std::span<float> out) {
    std::size_t offset = 0;
    for (const auto b : std::views::iota(std::size_t{0}, widths.size())) {
        const int width = widths[b];
        if (width <= 0) {
            out[b] = 0.0f;
        } else {
            const auto band_width = static_cast<std::size_t>(width);
            const float scale = 1.0f / static_cast<float>(width);
            const auto x_band = x.subspan(offset, band_width);

            float acc = 0.0f;
            for (const float val : x_band) {
                acc += val * scale;
            }
            out[b] = acc;
        }
        offset += static_cast<std::size_t>(width);
    }
}

void erb_interp_band_gain(std::span<float> out, std::span<const int> widths,
                          std::span<const float> gains) {
    std::size_t offset = 0;
    for (const auto b : std::views::iota(std::size_t{0}, widths.size())) {
        const int width = widths[b];
        if (width > 0) {
            const auto band_width = static_cast<std::size_t>(width);
            std::ranges::fill(out.subspan(offset, band_width), gains[b]);
            offset += band_width;
        }
    }
}

void erb_apply_interp_band_gain(std::span<std::complex<float>> spectrum,
                                std::span<const int> widths, std::span<const float> gains) {
    std::size_t offset = 0;
    for (const auto b : std::views::iota(std::size_t{0}, widths.size())) {
        const int width = widths[b];
        if (width > 0) {
            const auto band_width = static_cast<std::size_t>(width);
            const float gain = gains[b];
            for (auto& bin : spectrum.subspan(offset, band_width)) {
                bin *= gain;
            }
            offset += band_width;
        }
    }
}

}  // namespace dfn::dsp
