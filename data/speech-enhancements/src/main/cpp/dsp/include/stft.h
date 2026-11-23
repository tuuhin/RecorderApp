#ifndef DFN_DSP_STFT_H_
#define DFN_DSP_STFT_H_

// Streaming short-time Fourier transform.
//
// Reference: libDF/src/lib.rs, `frame_analysis`.
//
// For every hop of input samples the analysis window (length N) slides forward by `hop`.
// The current frame is windowed, the real FFT is taken and the spectrum is normalized by
// `analysis_window_norm` (2 * hop / N^2). The effective algorithmic latency of the
// analysis/synthesis loop is N - hop samples.

#include <complex>
#include <cstddef>
#include <memory>
#include <vector>

#include "fft.h"
#include "window.h"

namespace dfn::dsp {

struct StftConfig {
    int fft_size = 960;
    int hop_size = 480;
};

// Number of non-redundant frequency bins of a real FFT of `fft_size` samples.
int freq_size(int fft_size);

// Throws std::invalid_argument for configurations the DSP layer cannot represent
// (non-positive sizes, hop > N/2, or sizes unsupported by the FFT backend).
void validate_stft_config(const StftConfig& config);

class Stft {
   public:
    explicit Stft(StftConfig config);

    [[nodiscard]] const StftConfig& config() const { return config_; }
    [[nodiscard]] int fft_size() const { return config_.fft_size; }
    [[nodiscard]] int hop_size() const { return config_.hop_size; }
    [[nodiscard]] int n_freqs() const { return freq_size(config_.fft_size); }

    [[nodiscard]] const std::vector<float>& window() const { return window_.coefficients; }
    [[nodiscard]] float window_norm() const { return window_.norm; }

    void reset();

    // Consumes exactly hop_size() samples and writes n_freqs() complex bins.
    void process(const float* frame, std::complex<float>* spectrum);

    // Analyzes every complete frame of `signal` (trailing partial hop is dropped), starting
    // from the current streaming state. Returns the spectrogram as consecutive frames,
    // each holding n_freqs() complex bins.
    std::vector<std::complex<float>> analyze(const float* signal, std::size_t count,
                                             int* frame_count = nullptr);

   private:
    StftConfig config_;
    AnalysisWindow window_;
    std::vector<float> fft_input_;
    std::vector<float> analysis_mem_;
    std::unique_ptr<RealFft> fft_;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_STFT_H_
