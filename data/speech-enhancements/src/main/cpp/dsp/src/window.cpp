#include "window.h"

#include <cmath>

namespace dfn::dsp {

std::vector<float> make_vorbis_window(int fft_size) {
    std::vector<float> window(static_cast<std::size_t>(fft_size), 0.0f);
    if (fft_size <= 0) {
        return window;
    }
    const double pi = 3.14159265358979323846264338327950288;
    const auto half = static_cast<double>(fft_size) / 2;
    for (int i = 0; i < fft_size; ++i) {
        const double inner = std::sin(0.5 * pi * (static_cast<double>(i) + 0.5) / half);
        window[static_cast<std::size_t>(i)] =
            static_cast<float>(std::sin(0.5 * pi * inner * inner));
    }
    return window;
}

float analysis_window_norm(int fft_size, int hop_size) {
    if (fft_size <= 0) {
        return 1.0f;
    }
    const auto n = static_cast<float>(fft_size);
    const auto hop = static_cast<float>(hop_size);
    return 1.0f / (n * n / (2.0f * hop));
}

AnalysisWindow make_analysis_window(int fft_size, int hop_size) {
    AnalysisWindow window;
    window.coefficients = make_vorbis_window(fft_size);
    window.norm = analysis_window_norm(fft_size, hop_size);
    return window;
}

}  // namespace dfn::dsp
