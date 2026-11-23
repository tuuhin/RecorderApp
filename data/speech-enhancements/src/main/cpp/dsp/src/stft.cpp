#include "stft.h"

#include <algorithm>
#include <cstring>
#include <stdexcept>
#include <string>

namespace dfn::dsp {

int freq_size(int fft_size) {
    return fft_size / 2 + 1;
}

void validate_stft_config(const StftConfig& config) {
    if (config.fft_size < 32 || config.fft_size % 32 != 0) {
        throw std::invalid_argument(
            "StftConfig.fft_size must be a positive multiple of 32 (PFFFT requirement), got " +
            std::to_string(config.fft_size));
    }
    if (config.hop_size <= 0) {
        throw std::invalid_argument("StftConfig.hop_size must be positive");
    }
    if (2 * config.hop_size > config.fft_size) {
        throw std::invalid_argument(
            "StftConfig.hop_size must not exceed fft_size / 2 (upstream asserts "
            "hop_size * 2 <= fft_size)");
    }
}

Stft::Stft(StftConfig config) : config_(config) {
    validate_stft_config(config_);
    window_ = make_analysis_window(config_.fft_size, config_.hop_size);
    analysis_mem_.assign(static_cast<std::size_t>(config_.fft_size - config_.hop_size), 0.0f);
    fft_input_.assign(static_cast<std::size_t>(config_.fft_size), 0.0f);
    fft_ = std::make_unique<RealFft>(config_.fft_size);
}

void Stft::reset() {
    std::fill(analysis_mem_.begin(), analysis_mem_.end(), 0.0f);
}

void Stft::process(const float* frame, std::complex<float>* spectrum) {
    const int n = config_.fft_size;
    const int hop = config_.hop_size;
    const int history = n - hop;  // size of analysis_mem_
    const auto* window = window_.coefficients.data();

    // First part of the window applies to the samples carried over from previous frames.
    for (int i = 0; i < history; ++i) {
        fft_input_[static_cast<std::size_t>(i)] =
            analysis_mem_[static_cast<std::size_t>(i)] * window[i];
    }
    // Second part of the window applies to the new hop.
    for (int i = 0; i < hop; ++i) {
        fft_input_[static_cast<std::size_t>(history + i)] = frame[i] * window[history + i];
    }

    // Shift the analysis memory and store the new hop for the next call.
    const int overlap = history - hop;  // N - 2*hop
    if (overlap > 0) {
        std::memmove(analysis_mem_.data(),
                     analysis_mem_.data() + hop,
                     sizeof(float) * static_cast<std::size_t>(overlap));
    }
    std::copy(frame, frame + hop, analysis_mem_.begin() + overlap);

    fft_->forward(fft_input_.data(), spectrum);

    // Analysis-only normalization, exactly as in the reference implementation.
    const float norm = window_.norm;
    const int bins = freq_size(n);
    for (int k = 0; k < bins; ++k) {
        spectrum[k] *= norm;
    }
}

std::vector<std::complex<float>> Stft::analyze(const float* signal, std::size_t count,
                                               int* frame_count) {
    const int hop = config_.hop_size;
    const auto bins = static_cast<std::size_t>(n_freqs());
    const std::size_t frames = count / static_cast<std::size_t>(hop);
    std::vector<std::complex<float>> spectrogram(frames * bins);
    for (std::size_t f = 0; f < frames; ++f) {
        process(signal + f * static_cast<std::size_t>(hop), spectrogram.data() + f * bins);
    }
    if (frame_count != nullptr) {
        *frame_count = static_cast<int>(frames);
    }
    return spectrogram;
}

}  // namespace dfn::dsp
