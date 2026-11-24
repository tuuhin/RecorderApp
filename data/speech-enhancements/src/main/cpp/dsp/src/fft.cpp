#include "fft.h"

#include <pffft/pffft.h>

#include <algorithm>
#include <cstddef>
#include <format>
#include <new>
#include <ranges>
#include <span>
#include <stdexcept>

namespace dfn::dsp {
namespace {

constexpr int kMinPffftSize = 32;

void validate_fft_size(int fft_size) {
    if (fft_size < kMinPffftSize || fft_size % kMinPffftSize != 0)
        throw std::invalid_argument(
            std::format("PFFFT real transforms require a size that is a multiple of {}, got {}",
                        kMinPffftSize,
                        fft_size));
}

}  // namespace

void RealFft::SetupDeleter::operator()(PFFFT_Setup* setup) const noexcept {
    if (setup != nullptr) pffft_destroy_setup(setup);
}

void RealFft::AlignedBufferDeleter::operator()(float* ptr) const noexcept {
    if (ptr != nullptr) pffft_aligned_free(ptr);
}

RealFft::RealFft(int fft_size) : fft_size_(fft_size) {
    validate_fft_size(fft_size);
    _setup.reset(pffft_new_setup(fft_size, PFFFT_REAL));

    if (_setup == nullptr)
        throw std::invalid_argument(
            std::format("pffft_new_setup failed for real transform size {} "
                        "(PFFFT supports N = 2^a * 3^b * 5^c with a >= 5)",
                        fft_size));

    const auto count = static_cast<std::size_t>(fft_size);
    _packed.reset(static_cast<float*>(pffft_aligned_malloc(sizeof(float) * count)));
    _work.reset(static_cast<float*>(pffft_aligned_malloc(sizeof(float) * count)));
    if (!_packed || !_work) throw std::bad_alloc();
}

void RealFft::forward(std::span<const float> input, std::span<std::complex<float>> output) {
    const auto n = static_cast<std::size_t>(fft_size_);
    const auto half = static_cast<std::size_t>(fft_size_ / 2);
    const auto bins = static_cast<std::size_t>(freq_size());

    const auto in_span = input.first(n);
    const auto out_span = output.first(bins);
    const auto packed_span = std::span{_packed.get(), n};
    const auto work_span = std::span{_work.get(), n};

    pffft_transform_ordered(
        _setup.get(), in_span.data(), packed_span.data(), work_span.data(), PFFFT_FORWARD);

    // PFFFT's ordered real layout stores fft_size()/2 complex values:
    //   packed[0] = Re(X[0]),  packed[1] = Re(X[N/2]),
    //   packed[2*k] = Re(X[k]), packed[2*k+1] = Im(X[k]) for k = 1 .. N/2 - 1.
    out_span[0] = {packed_span[0], 0.0f};
    out_span[half] = {packed_span[1], 0.0f};

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{1}, half)) {
        out_span[k] = {packed_span[2 * k], packed_span[2 * k + 1]};
    }
}

void RealFft::backward(std::span<const std::complex<float>> input, std::span<float> output) {
    const auto n = static_cast<std::size_t>(fft_size_);
    const auto half = static_cast<std::size_t>(fft_size_ / 2);
    const auto bins = static_cast<std::size_t>(freq_size());

    const auto in_span = input.first(bins);
    const auto out_span = output.first(n);
    const auto packed_span = std::span{_packed.get(), n};
    const auto work_span = std::span{_work.get(), n};

    packed_span[0] = in_span[0].real();
    packed_span[1] = in_span[half].real();

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{1}, half)) {
        packed_span[2 * k] = in_span[k].real();
        packed_span[2 * k + 1] = in_span[k].imag();
    }

    pffft_transform_ordered(
        _setup.get(), packed_span.data(), work_span.data(), nullptr, PFFFT_BACKWARD);
    std::ranges::copy(work_span, out_span.begin());
}

}  // namespace dfn::dsp
