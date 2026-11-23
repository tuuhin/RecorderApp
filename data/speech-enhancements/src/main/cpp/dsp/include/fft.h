#ifndef DFN_DSP_FFT_H_
#define DFN_DSP_FFT_H_

// Real-valued FFT backend.
//
// This header is the only place in the DSP layer that touches PFFFT. Keeping the FFT
// implementation behind this interface means the rest of the DSP code is written against
// conventional complex spectra and can be tested without knowing PFFFT's packing rules.
//
// Reference: libDF/src/lib.rs (DFState) uses `realfft` (rustfft) with an unnormalized
// forward transform and an unnormalized inverse transform that yields a factor of N.

#include <complex>
#include <cstddef>
#include <vector>

struct PFFFT_Setup;

namespace dfn::dsp {

// Forward and inverse real FFT of a fixed power-of-two-times-3-5 size.
//
// Conventions (identical to rustfft as used by libDF):
//   forward(x)[k]  = sum_n x[n] * exp(-2*pi*i*k*n/N)
//   backward(X)[n] = sum_k X[k] * exp(+2*pi*i*k*n/N)      (i.e. N * IFFT)
// Transforms are not scaled.
class RealFft {
   public:
    explicit RealFft(int fft_size);
    ~RealFft();

    RealFft(const RealFft&) = delete;
    RealFft& operator=(const RealFft&) = delete;
    RealFft(RealFft&&) = delete;
    RealFft& operator=(RealFft&&) = delete;

    [[nodiscard]] int fft_size() const { return fft_size_; }
    [[nodiscard]] int freq_size() const { return fft_size_ / 2 + 1; }

    // `input` holds fft_size() real samples. `output` holds freq_size() complex bins.
    void forward(const float* input, std::complex<float>* output);

    // `input` holds freq_size() complex bins (imaginary part of DC and Nyquist is ignored).
    // `output` holds fft_size() real samples, scaled by fft_size().
    void backward(const std::complex<float>* input, float* output);

   private:
    int fft_size_;
    PFFFT_Setup* setup_ = nullptr;
    float* packed_ = nullptr;  // N floats in PFFFT's ordered real layout
    float* work_ = nullptr;    // N floats of scratch space
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_FFT_H_
