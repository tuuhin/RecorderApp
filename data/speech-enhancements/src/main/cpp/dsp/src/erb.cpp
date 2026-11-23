#include "erb.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <string>

namespace dfn::dsp {

float erb_from_hz(float freq_hz) {
    return 9.265f * std::log1p(freq_hz / (24.7f * 9.265f));
}

float hz_from_erb(float erb) {
    return 24.7f * 9.265f * std::expm1(erb / 9.265f);
}

std::vector<int> erb_band_widths(int sample_rate, int fft_size, int nb_bands, int min_nb_freqs) {
    if (sample_rate <= 0 || fft_size <= 0 || nb_bands <= 0) {
        throw std::invalid_argument(
            "erb_band_widths requires positive sample rate, fft size, bands");
    }
    if (min_nb_freqs <= 0) {
        throw std::invalid_argument("erb_band_widths requires min_nb_freqs > 0");
    }
    const int nyq_freq = sample_rate / 2;
    const auto freq_width = static_cast<float>(sample_rate) / static_cast<float>(fft_size);
    const float erb_low = erb_from_hz(0.0f);
    const float erb_high = erb_from_hz(static_cast<float>(nyq_freq));
    const float step = (erb_high - erb_low) / static_cast<float>(nb_bands);

    std::vector<int> widths(static_cast<std::size_t>(nb_bands), 0);
    int prev_freq = 0;  // last frequency bin of the previous band
    int freq_over = 0;  // bins already forced into previous bands
    for (int i = 1; i <= nb_bands; ++i) {
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
    widths[static_cast<std::size_t>(nb_bands - 1)] += 1;

    int total = 0;
    for (const int width : widths) {
        total += width;
    }
    const int too_large = total - (fft_size / 2 + 1);
    if (too_large > 0) {
        widths[static_cast<std::size_t>(nb_bands - 1)] -= too_large;
    }
    return widths;
}

int erb_total_width(const std::vector<int>& widths) {
    int total = 0;
    for (const int width : widths) {
        total += width;
    }
    return total;
}

void erb_band_correlation(const std::complex<float>* x, const std::complex<float>* p,
                          const std::vector<int>& widths, float* out) {
    int offset = 0;
    for (std::size_t b = 0; b < widths.size(); ++b) {
        const int width = widths[b];
        if (width <= 0) {
            out[b] = 0.0f;
        } else {
            const float k = 1.0f / static_cast<float>(width);
            float acc = 0.0f;
            for (int j = 0; j < width; ++j) {
                const int idx = offset + j;
                acc += (x[idx].real() * p[idx].real() + x[idx].imag() * p[idx].imag()) * k;
            }
            out[b] = acc;
        }
        offset += width;
    }
}

void erb_band_compress(const float* x, const std::vector<int>& widths, float* out) {
    int offset = 0;
    for (std::size_t b = 0; b < widths.size(); ++b) {
        const int width = widths[b];
        if (width <= 0) {
            out[b] = 0.0f;
        } else {
            const float k = 1.0f / static_cast<float>(width);
            float acc = 0.0f;
            for (int j = 0; j < width; ++j) {
                acc += x[offset + j] * k;
            }
            out[b] = acc;
        }
        offset += width;
    }
}

void erb_interp_band_gain(float* out, const std::vector<int>& widths, const float* gains) {
    int offset = 0;
    for (std::size_t b = 0; b < widths.size(); ++b) {
        for (int j = 0; j < widths[b]; ++j) {
            out[offset + j] = gains[b];
        }
        offset += widths[b];
    }
}

void erb_apply_interp_band_gain(std::complex<float>* spectrum, const std::vector<int>& widths,
                                const float* gains) {
    int offset = 0;
    for (std::size_t b = 0; b < widths.size(); ++b) {
        const float gain = gains[b];
        for (int j = 0; j < widths[b]; ++j) {
            spectrum[offset + j] *= gain;
        }
        offset += widths[b];
    }
}

}  // namespace dfn::dsp
