#include "fft.h"

#include <pffft/pffft.h>

#include <algorithm>
#include <cstring>
#include <stdexcept>
#include <string>

namespace dfn::dsp {
namespace {

void validate_fft_size(int fft_size) {
    if (fft_size < 32 || fft_size % 32 != 0) {
        throw std::invalid_argument(
            "PFFFT real transforms require a size that is a multiple of 32, got " +
            std::to_string(fft_size));
    }
}

}  // namespace

RealFft::RealFft(int fft_size) : fft_size_(fft_size) {
    validate_fft_size(fft_size);
    setup_ = pffft_new_setup(fft_size, PFFFT_REAL);
    if (setup_ == nullptr) {
        throw std::invalid_argument("pffft_new_setup failed for real transform size " +
                                    std::to_string(fft_size) +
                                    " (PFFFT supports N = 2^a * 3^b * 5^c with a >= 5)");
    }
    packed_ =
        static_cast<float*>(pffft_aligned_malloc(sizeof(float) * static_cast<size_t>(fft_size)));
    work_ =
        static_cast<float*>(pffft_aligned_malloc(sizeof(float) * static_cast<size_t>(fft_size)));
    if (packed_ == nullptr || work_ == nullptr) {
        pffft_destroy_setup(setup_);
        setup_ = nullptr;
        throw std::bad_alloc();
    }
}

RealFft::~RealFft() {
    if (packed_ != nullptr) {
        pffft_aligned_free(packed_);
    }
    if (work_ != nullptr) {
        pffft_aligned_free(work_);
    }
    if (setup_ != nullptr) {
        pffft_destroy_setup(setup_);
    }
}

void RealFft::forward(const float* input, std::complex<float>* output) {
    pffft_transform_ordered(setup_, input, packed_, work_, PFFFT_FORWARD);
    // PFFFT's ordered real layout stores fft_size()/2 complex values:
    //   packed[0] = Re(X[0]),  packed[1] = Re(X[N/2]),
    //   packed[2*k] = Re(X[k]), packed[2*k+1] = Im(X[k]) for k = 1 .. N/2 - 1.
    const int half = fft_size_ / 2;
    output[0] = std::complex<float>(packed_[0], 0.0f);
    output[half] = std::complex<float>(packed_[1], 0.0f);
    for (int k = 1; k < half; ++k) {
        output[k] = std::complex<float>(packed_[2 * k], packed_[2 * k + 1]);
    }
}

void RealFft::backward(const std::complex<float>* input, float* output) {
    const int half = fft_size_ / 2;
    packed_[0] = input[0].real();
    packed_[1] = input[half].real();
    for (int k = 1; k < half; ++k) {
        packed_[2 * k] = input[k].real();
        packed_[2 * k + 1] = input[k].imag();
    }
    pffft_transform_ordered(setup_, packed_, work_, nullptr, PFFFT_BACKWARD);
    std::memcpy(output, work_, sizeof(float) * static_cast<size_t>(fft_size_));
}

}  // namespace dfn::dsp
