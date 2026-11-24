#include "audio_frame.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <ranges>
#include <span>
#include <vector>

namespace dfn::dsp {

std::int16_t float_sample_to_pcm16(float sample) noexcept {
    constexpr auto kMinInt16 = std::numeric_limits<std::int16_t>::min();
    constexpr auto kMaxInt16 = std::numeric_limits<std::int16_t>::max();
    constexpr auto kMinFloat = static_cast<float>(kMinInt16);
    constexpr auto kMaxFloat = static_cast<float>(kMaxInt16);

    // NaN cannot be represented; infinities saturate like any other out-of-range value.
    if (!std::isfinite(sample))
        return std::isnan(sample) ? std::int16_t{0} : (sample > 0.0f ? kMaxInt16 : kMinInt16);

    const float scaled = sample * kPcm16Scale;
    // `static_cast<std::int16_t>` truncates toward zero, matching torch's int16 cast for
    // values inside the representable range.
    const float clamped = std::clamp(scaled, kMinFloat, kMaxFloat);
    return static_cast<std::int16_t>(clamped);
}

void pcm16_to_float(std::span<const std::int16_t> input, std::span<float> output) {
    const auto out_span = output.first(input.size());
    std::ranges::transform(input, out_span.begin(), pcm16_sample_to_float);
}

void float_to_pcm16(std::span<const float> input, std::span<std::int16_t> output) {
    const auto out_span = output.first(input.size());
    std::ranges::transform(input, out_span.begin(), float_sample_to_pcm16);
}

std::vector<float> pcm16_to_float(std::span<const std::int16_t> input) {
    std::vector<float> output(input.size());
    pcm16_to_float(input, output);
    return output;
}

std::vector<std::int16_t> float_to_pcm16(std::span<const float> input) {
    std::vector<std::int16_t> output(input.size());
    float_to_pcm16(input, output);
    return output;
}

frame_splitter::frame_splitter(int hop_size) noexcept : hop_size_(hop_size > 0 ? hop_size : 1) {
    pending_.reserve(static_cast<std::size_t>(hop_size_) * 2);
}

void frame_splitter::reset() noexcept {
    pending_.clear();
}

std::size_t frame_splitter::push(std::span<const float> input, std::vector<float>& frames) {
    if (input.empty()) {
        return 0;
    }

    const auto hop = static_cast<std::size_t>(hop_size_);
    std::size_t frames_appended = 0;
    std::size_t consumed = 0;

    // Fill the pending tail first.
    if (!pending_.empty()) {
        const std::size_t needed = hop - pending_.size();
        const std::size_t take = std::min(needed, input.size());
        const auto tail = input.first(take);
        pending_.insert(pending_.end(), tail.begin(), tail.end());
        consumed += take;

        if (pending_.size() == hop) {
            frames.insert(frames.end(), pending_.begin(), pending_.end());
            pending_.clear();
            ++frames_appended;
        }
    }

    // Emit complete frames straight from the input buffer.
    while (input.size() - consumed >= hop) {
        const auto chunk = input.subspan(consumed, hop);
        frames.insert(frames.end(), chunk.begin(), chunk.end());
        consumed += hop;
        ++frames_appended;
    }

    // Keep the remainder for the next call.
    if (consumed < input.size()) {
        const auto rem = input.subspan(consumed);
        pending_.insert(pending_.end(), rem.begin(), rem.end());
    }

    return frames_appended;
}

bool frame_splitter::flush(std::vector<float>& frames) {
    if (pending_.empty()) {
        return false;
    }

    const auto hop = static_cast<std::size_t>(hop_size_);
    const std::size_t offset = frames.size();
    frames.insert(frames.end(), pending_.begin(), pending_.end());
    frames.resize(offset + hop, 0.0f);
    pending_.clear();
    return true;
}

}  // namespace dfn::dsp
