#include "istft.h"

#include <algorithm>
#include <cstring>

namespace dfn::dsp {

Istft::Istft(StftConfig config) : stft_config_(config) {
    validate_stft_config(stft_config_);
    window_ = make_analysis_window(stft_config_.fft_size, stft_config_.hop_size);
    synthesis_mem_.assign(static_cast<std::size_t>(stft_config_.fft_size - stft_config_.hop_size),
                          0.0f);
    fft_output_.assign(static_cast<std::size_t>(stft_config_.fft_size), 0.0f);
    fft_ = std::make_unique<RealFft>(stft_config_.fft_size);
}

void Istft::reset() {
    std::fill(synthesis_mem_.begin(), synthesis_mem_.end(), 0.0f);
}

void Istft::process(const std::complex<float>* spectrum, float* frame) {
    const int n = stft_config_.fft_size;
    const int hop = stft_config_.hop_size;
    const int history = n - hop;

    fft_->backward(spectrum, fft_output_.data());

    // Synthesis window (the analysis normalization is already part of the spectrum).
    const auto* window = window_.coefficients.data();
    for (int i = 0; i < n; ++i) {
        fft_output_[static_cast<std::size_t>(i)] *= window[i];
    }

    // Emit the overlap of the current frame with the buffered tail.
    for (int i = 0; i < hop; ++i) {
        frame[i] =
            fft_output_[static_cast<std::size_t>(i)] + synthesis_mem_[static_cast<std::size_t>(i)];
    }

    // Slide the overlap memory forward by one hop.
    const int overlap = history - hop;  // N - 2*hop
    if (overlap > 0) {
        std::memmove(synthesis_mem_.data(),
                     synthesis_mem_.data() + hop,
                     sizeof(float) * static_cast<std::size_t>(overlap));
    }
    // Accumulate the second half of the frame (overlap-add) and store the tail.
    for (int i = 0; i < overlap; ++i) {
        synthesis_mem_[static_cast<std::size_t>(i)] +=
            fft_output_[static_cast<std::size_t>(hop + i)];
    }
    for (int i = overlap; i < history; ++i) {
        synthesis_mem_[static_cast<std::size_t>(i)] =
            fft_output_[static_cast<std::size_t>(hop + i)];
    }
}

std::vector<float> Istft::synthesize(const std::complex<float>* spectrogram, int frame_count) {
    const int hop = stft_config_.hop_size;
    const auto bins = static_cast<std::size_t>(n_freqs());
    std::vector<float> output(static_cast<std::size_t>(frame_count) *
                              static_cast<std::size_t>(hop));
    for (int f = 0; f < frame_count; ++f) {
        process(spectrogram + static_cast<std::size_t>(f) * bins,
                output.data() + static_cast<std::size_t>(f) * static_cast<std::size_t>(hop));
    }
    return output;
}

}  // namespace dfn::dsp
