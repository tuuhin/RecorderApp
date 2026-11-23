#include "audio_frame.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace dfn::dsp {

float pcm16_sample_to_float(int16_t sample) {
    return static_cast<float>(sample) * kPcm16InvScale;
}

int16_t float_sample_to_pcm16(float sample) {
    if (!std::isfinite(sample)) {
        // NaN cannot be represented; infinities saturate like any other out-of-range value.
        return std::isnan(sample) ? 0 : (sample > 0.0f ? 32767 : -32768);
    }
    const float scaled = sample * kPcm16Scale;
    // `static_cast<int16_t>` truncates toward zero, matching torch's int16 cast for values
    // inside the representable range.
    const float clamped = std::min(std::max(scaled, -32768.0f), 32767.0f);
    return static_cast<int16_t>(clamped);
}

void pcm16_to_float(const int16_t* input, std::size_t count, float* output) {
    for (std::size_t i = 0; i < count; ++i) {
        output[i] = pcm16_sample_to_float(input[i]);
    }
}

void float_to_pcm16(const float* input, std::size_t count, int16_t* output) {
    for (std::size_t i = 0; i < count; ++i) {
        output[i] = float_sample_to_pcm16(input[i]);
    }
}

std::vector<float> pcm16_to_float(std::span<const int16_t> input) {
    std::vector<float> output(input.size());
    pcm16_to_float(input.data(), input.size(), output.data());
    return output;
}

std::vector<int16_t> float_to_pcm16(std::span<const float> input) {
    std::vector<int16_t> output(input.size());
    float_to_pcm16(input.data(), input.size(), output.data());
    return output;
}

frame_splitter::frame_splitter(int hop_size) : hop_size_(hop_size) {
    if (hop_size_ <= 0) {
        hop_size_ = 1;
    }
    pending_.reserve(static_cast<std::size_t>(hop_size_) * 2);
}

void frame_splitter::reset() {
    pending_.clear();
}

std::size_t frame_splitter::push(const float* input, std::size_t count,
                                 std::vector<float>& frames) {
    if (input == nullptr || count == 0) {
        return 0;
    }
    const auto hop = static_cast<std::size_t>(hop_size_);
    std::size_t frames_appended = 0;
    std::size_t consumed = 0;

    // Fill the pending tail first.
    if (!pending_.empty()) {
        const std::size_t needed = hop - pending_.size();
        const std::size_t take = std::min(needed, count);
        pending_.insert(pending_.end(), input, input + take);
        consumed += take;
        if (pending_.size() == hop) {
            frames.insert(frames.end(), pending_.begin(), pending_.end());
            pending_.clear();
            ++frames_appended;
        }
    }

    // Emit complete frames straight from the input buffer.
    while (count - consumed >= hop) {
        frames.insert(frames.end(), input + consumed, input + consumed + hop);
        consumed += hop;
        ++frames_appended;
    }

    // Keep the remainder for the next call.
    if (consumed < count) {
        pending_.insert(pending_.end(), input + consumed, input + count);
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
