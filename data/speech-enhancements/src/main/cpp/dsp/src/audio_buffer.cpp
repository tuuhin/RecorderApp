#include "audio_buffer.h"

#include <algorithm>
#include <cstddef>
#include <cstring>
#include <numeric>
#include <span>
#include <vector>

namespace dfn::dsp {

audio_buffer::audio_buffer(std::size_t capacity) : data_(capacity) {}

void audio_buffer::reset() noexcept {
    head_ = 0;
    size_ = 0;
}

void audio_buffer::push(std::span<const float> data) {
    if (data.empty()) {
        return;
    }

    const std::size_t count = data.size();

    // Ensure the data stays contiguous at the front. Compact first, then grow.
    if (head_ + size_ + count > data_.size()) {
        if (count <= data_.size()) {
            compact();
        }
        if (size_ + count > data_.size()) {
            data_.resize(size_ + count);
        }
    }

    auto dst = std::span{data_}.subspan(head_ + size_, count);
    std::ranges::copy(data, dst.begin());
    size_ += count;
}

std::size_t audio_buffer::pop(std::span<float> out, std::size_t count) {
    const std::size_t n = std::min(count, size_);
    if (n == 0) {
        return 0;
    }

    if (!out.empty()) {
        const std::size_t to_copy = std::min(n, out.size());
        auto src = std::span{data_}.subspan(head_, to_copy);
        std::ranges::copy(src, out.first(to_copy).begin());
    }

    head_ += n;
    size_ -= n;
    if (size_ == 0) {
        head_ = 0;
    } else if (head_ > std::midpoint(std::size_t{0}, data_.size())) {
        compact();
    }

    return n;
}

std::size_t audio_buffer::discard(std::size_t count) {
    return pop(std::span<float>{}, count);
}

std::span<const float> audio_buffer::view() const {
    compact();
    return std::span{data_}.first(size_);
}

const float* audio_buffer::data() const {
    return view().data();
}

std::size_t audio_buffer::peek(std::span<float> out, std::size_t offset, std::size_t count) const {
    if (offset >= size_) {
        return 0;
    }

    const std::size_t n = std::min(count, size_ - offset);
    if (!out.empty()) {
        const std::size_t to_copy = std::min(n, out.size());
        auto src = std::span{data_}.subspan(head_ + offset, to_copy);
        std::ranges::copy(src, out.first(to_copy).begin());
    }
    return n;
}

void audio_buffer::compact() const {
    if (head_ == 0 || size_ == 0) {
        head_ = 0;
        return;
    }
    if (head_ + size_ > data_.size()) {
        // Defensive: with a valid invariant this cannot happen, but keep the buffer usable.
        data_.resize(head_ + size_);
    }
    // Overlapping left shift: std::memmove is guaranteed safe for overlapping regions.
    std::memmove(data_.data(), data_.data() + head_, sizeof(float) * size_);
    head_ = 0;
}

}  // namespace dfn::dsp
