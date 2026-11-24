#ifndef DFN_DSP_ISTFT_H_
#define DFN_DSP_ISTFT_H_

// Streaming inverse short-time Fourier transform with weighted overlap-add.
//
// Reference: libDF/src/lib.rs, `frame_synthesis`.
//
// Each hop: inverse real FFT (unnormalized), multiply by the synthesis window, add the first
// hop to the overlap memory, emit it, then fold the remainder into the memory.

#include <complex>
#include <cstddef>
#include <memory>
#include <vector>

#include "fft.h"
#include "stft.h"

namespace dfn::dsp {

class Istft {
   public:
    explicit Istft(stft_config config);

    const stft_config& config() const { return stft_config_; }
    int fft_size() const { return stft_config_.fft_size; }
    int hop_size() const { return stft_config_.hop_size; }
    int n_freqs() const { return freq_size(stft_config_.fft_size); }

    const std::vector<float>& window() const { return window_.coefficients; }

    void reset();

    // Consumes one spectrum of n_freqs() bins and writes exactly hop_size() samples.
    void process(const std::complex<float>* spectrum, float* frame);

    // Reconstructs `count` input samples supplied as a spectrogram of `count / hop` frames.
    // The result is delayed by (fft_size - hop_size) samples; the returned buffer holds
    // frames * hop_size samples.
    std::vector<float> synthesize(const std::complex<float>* spectrogram, int frame_count);

   private:
    stft_config stft_config_;
    analysis_window window_;
    std::vector<float> synthesis_mem_;
    std::vector<float> fft_output_;
    std::unique_ptr<RealFft> fft_;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_ISTFT_H_
