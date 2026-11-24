#include "window.h"

#include <numbers>
#include <ranges>
#include <vector>

namespace dfn::dsp {

std::vector<float> make_vorbis_window(int fft_size) {
    if (fft_size <= 0) return {};

    const auto size = static_cast<std::size_t>(fft_size);
    const auto half = static_cast<double>(fft_size) * 0.5;
    constexpr double half_pi = 0.5 * std::numbers::pi;

    std::vector<float> window;
    window.reserve(size);

    for (const std::weakly_incrementable auto i : std::views::iota(0, fft_size)) {
        const double phase = half_pi * (static_cast<double>(i) + 0.5) / half;
        const double inner = std::sin(phase);
        window.push_back(static_cast<float>(std::sin(half_pi * inner * inner)));
    }

    return window;
}

float analysis_window_norm(int fft_size, int hop_size) {
    if (fft_size <= 0 || hop_size <= 0) return 1.0f;

    const auto n = static_cast<float>(fft_size);
    const auto hop = static_cast<float>(hop_size);
    return (2.0f * hop) / (n * n);
}

analysis_window make_analysis_window(int fft_size, int hop_size) {
    analysis_window window;
    window.coefficients = make_vorbis_window(fft_size);
    window.norm = analysis_window_norm(fft_size, hop_size);
    return window;
}

}  // namespace dfn::dsp
