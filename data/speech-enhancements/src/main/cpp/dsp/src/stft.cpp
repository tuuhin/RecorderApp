#include "stft.h"

#include <algorithm>
#include <complex>
#include <cstddef>
#include <format>
#include <memory>
#include <numeric>
#include <span>
#include <stdexcept>
#include <vector>

namespace dfn::dsp {

int freq_size(int fft_size) {
    return fft_size / 2 + 1;
}

void validate_stft_config(const stft_config& config) {
    constexpr int min_fft_size = 32;
    // ensure its correct size
    if (config.fft_size < min_fft_size || config.fft_size % min_fft_size != 0)
        throw std::invalid_argument(std::format(
            "StftConfig.fft_size must be a positive multiple of {} (PFFFT requirement), got {}",
            min_fft_size,
            config.fft_size));

    // ensure corrected hop size
    if (config.hop_size <= 0) throw std::invalid_argument("StftConfig.hop_size must be positive");

    // fft size is greater than hop
    if (2 * config.hop_size > config.fft_size)
        throw std::invalid_argument(
            "StftConfig.hop_size must not exceed fft_size / 2 (upstream asserts "
            "hop_size * 2 <= fft_size)");
}

Stft::Stft(stft_config config) : _stft_config(config) {
    validate_stft_config(_stft_config);
    _analysis_window = make_analysis_window(_stft_config.fft_size, _stft_config.hop_size);
    const auto history = static_cast<std::size_t>(_stft_config.fft_size - _stft_config.hop_size);
    const auto fft_len = static_cast<std::size_t>(_stft_config.fft_size);
    _analysis_memory.assign(history, 0.0f);
    _input.assign(fft_len, 0.0f);
    _fft = std::make_unique<RealFft>(_stft_config.fft_size);
}

void Stft::reset() {
    std::ranges::fill(_analysis_memory, 0.0f);
}

void Stft::process(const float* frame, std::complex<float>* spectrum) {
    const auto n = static_cast<std::size_t>(_stft_config.fft_size);
    const auto hop = static_cast<std::size_t>(_stft_config.hop_size);
    const auto history = n - hop;
    const auto bins = static_cast<std::size_t>(freq_size(_stft_config.fft_size));

    const auto in_frame = std::span{frame, hop};
    const auto out_spectrum = std::span{spectrum, bins};
    const auto win = std::span{_analysis_window.coefficients.data(), n};
    auto mem = std::span{_analysis_memory};
    auto fft_in = std::span{_input};

    // First part of the window applies to samples carried over from previous frames.
    std::transform(mem.begin(), mem.end(), win.begin(), fft_in.begin(), std::multiplies<>{});

    // Second part of the window applies to the new hop.
    std::transform(in_frame.begin(),
                   in_frame.end(),
                   win.subspan(history).begin(),
                   fft_in.subspan(history).begin(),
                   std::multiplies<>{});

    // Shift analysis memory and store the new hop for the next call.
    const auto overlap = history - hop;  // N - 2*hop
    if (overlap > 0) std::ranges::copy(mem.subspan(hop, overlap), mem.begin());

    std::ranges::copy(in_frame, mem.subspan(overlap).begin());
    _fft->forward(fft_in, out_spectrum);

    // Analysis-only normalization, exactly as in the reference implementation.
    const float norm = _analysis_window.norm;
    for (auto& bin : out_spectrum) {
        bin *= norm;
    }
}

std::vector<std::complex<float>> Stft::analyze(const float* signal, std::size_t count,
                                               int* frame_count) {
    const auto hop = static_cast<std::size_t>(_stft_config.hop_size);
    const auto bins = static_cast<std::size_t>(n_freqs());
    const std::size_t frames = count / hop;
    const auto sig = std::span{signal, count};

    std::vector<std::complex<float>> spectrogram(frames * bins);
    auto spec = std::span{spectrogram};

    for (std::size_t f = 0; f < frames; ++f) {
        const auto frame_offset = f * hop;
        const auto bin_offset = f * bins;
        process(sig.subspan(frame_offset, hop).data(), spec.subspan(bin_offset).data());
    }

    if (frame_count != nullptr) *frame_count = static_cast<int>(frames);

    return spectrogram;
}

}  // namespace dfn::dsp
