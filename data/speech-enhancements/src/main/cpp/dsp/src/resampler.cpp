#include "resampler.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <numbers>
#include <ranges>
#include <span>
#include <stdexcept>

namespace dfn::dsp {
namespace {

constexpr int kTaps = 16;            // kernel half-width in input samples
constexpr double kRolloff = 0.95;    // cutoff as a fraction of the smaller Nyquist frequency
constexpr double kKaiserBeta = 8.6;  // kaiser_fast-like window shape

// Modified Bessel function of the first kind, order 0 (series expansion; converges quickly
// for the beta values used here).
[[nodiscard]] double bessel_i0(double x) noexcept {
    double sum = 1.0;
    double term = 1.0;
    const double half_x_sq = (x * x) / 4.0;
    for (const int k : std::views::iota(1, 64)) {
        term *= half_x_sq / static_cast<double>(k * k);
        sum += term;
        if (term < sum * 1e-17) {
            break;
        }
    }
    return sum;
}

[[nodiscard]] double sinc(double x) noexcept {
    if (std::abs(x) < 1e-12) {
        return 1.0;
    }
    const double arg = std::numbers::pi * x;
    return std::sin(arg) / arg;
}

}  // namespace

Resampler::Resampler(int input_rate, int output_rate)
    : input_rate_(input_rate), output_rate_(output_rate) {
    if (input_rate_ <= 0 || output_rate_ <= 0) {
        throw std::invalid_argument("Resampler requires positive sample rates");
    }
    ratio_ = static_cast<double>(input_rate_) / static_cast<double>(output_rate_);
    history_.reserve(4096);
}

void Resampler::reset() noexcept {
    history_.clear();
    history_head_ = 0;
    history_start_ = 0;
    total_input_ = 0;
    next_output_ = 0;
}

std::size_t Resampler::max_output_samples(std::size_t input_count) const {
    const double produced = static_cast<double>(input_count) * static_cast<double>(output_rate_) /
                            static_cast<double>(input_rate_);
    return static_cast<std::size_t>(std::ceil(produced)) + static_cast<std::size_t>(kTaps) + 4;
}

std::size_t Resampler::output_length(std::size_t input_count) const {
    if (is_identity()) {
        return input_count;
    }
    const double produced = static_cast<double>(input_count) * static_cast<double>(output_rate_) /
                            static_cast<double>(input_rate_);
    return static_cast<std::size_t>(std::llround(produced));
}

std::size_t Resampler::process(std::span<const float> input, std::span<float> out) {
    if (is_identity()) {
        const std::size_t n = std::min(input.size(), out.size());
        std::ranges::copy(input.first(n), out.begin());
        total_input_ += input.size();
        next_output_ += n;
        return n;
    }

    if (!input.empty()) {
        const std::size_t available = history_.size() - history_head_;
        // Reuse the prefix of the vector when it has been consumed.
        if (history_head_ > 4096 && history_head_ > available) {
            std::memmove(
                history_.data(), history_.data() + history_head_, sizeof(float) * available);
            history_.resize(available);
            history_head_ = 0;
        }
        history_.insert(history_.end(), input.begin(), input.end());
        total_input_ += input.size();
    }

    std::size_t written = 0;
    const std::int64_t last_available = static_cast<std::int64_t>(total_input_) - 1;
    while (written < out.size()) {
        const double position = static_cast<double>(next_output_) * ratio_;
        const auto center = static_cast<std::int64_t>(std::floor(position));
        // Need samples up to center + kTaps to compute this output.
        if (center + kTaps > last_available) {
            break;
        }
        out[written++] = interpolate(position);
        ++next_output_;
        trim_history(
            static_cast<std::int64_t>(std::floor(static_cast<double>(next_output_) * ratio_)));
    }
    return written;
}

std::size_t Resampler::flush(std::span<float> out) {
    const std::size_t target = output_length(total_input_);
    std::size_t written = 0;
    while (written < out.size() && next_output_ < target) {
        const double position = static_cast<double>(next_output_) * ratio_;
        out[written++] = interpolate(position);
        ++next_output_;
    }
    return written;
}

float Resampler::sample_at(std::int64_t index) const noexcept {
    if (index < 0 || index < history_start_) {
        return 0.0f;
    }
    const std::size_t available = history_.size() - history_head_;
    const auto offset = static_cast<std::int64_t>(index) - history_start_;
    if (offset < 0 || static_cast<std::size_t>(offset) >= available) {
        return 0.0f;
    }
    return history_[history_head_ + static_cast<std::size_t>(offset)];
}

float Resampler::interpolate(double position) const {
    const auto center = static_cast<std::int64_t>(std::floor(position));
    const double cutoff =
        0.5 * std::min(1.0, static_cast<double>(output_rate_) / static_cast<double>(input_rate_)) *
        kRolloff;
    const double i0_beta = bessel_i0(kKaiserBeta);
    double acc = 0.0;
    double weight_sum = 0.0;

    for (const int j : std::views::iota(-kTaps, kTaps + 1)) {
        const auto index = center + j;
        const double x = position - static_cast<double>(index);  // in input samples
        const double u = x / static_cast<double>(kTaps);
        if (u <= -1.0 || u >= 1.0) {
            continue;
        }
        const double window = bessel_i0(kKaiserBeta * std::sqrt(1.0 - u * u)) / i0_beta;
        const double weight = 2.0 * cutoff * sinc(2.0 * cutoff * x) * window;
        acc += weight * static_cast<double>(sample_at(index));
        weight_sum += weight;
    }

    if (std::abs(weight_sum) < 1e-12) {
        return 0.0f;
    }
    return static_cast<float>(acc / weight_sum);
}

void Resampler::trim_history(std::int64_t next_position) {
    const std::int64_t keep_from = std::max<std::int64_t>(0, next_position - kTaps);
    if (keep_from <= history_start_) {
        return;
    }
    const std::size_t available = history_.size() - history_head_;
    const auto advance = static_cast<std::size_t>(keep_from - history_start_);
    if (advance >= available) {
        history_.clear();
        history_head_ = 0;
        history_start_ = keep_from;
        return;
    }
    history_head_ += advance;
    history_start_ = keep_from;
    if (history_head_ > 8192) {
        const std::size_t remaining = history_.size() - history_head_;
        std::memmove(history_.data(), history_.data() + history_head_, sizeof(float) * remaining);
        history_.resize(remaining);
        history_head_ = 0;
    }
}

}  // namespace dfn::dsp
