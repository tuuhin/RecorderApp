#include "audio_buffer.h"

#include <algorithm>
#include <cstring>

namespace dfn::dsp {

audio_buffer::audio_buffer(std::size_t capacity) {
    data_.resize(capacity);
}

void audio_buffer::reset() {
    head_ = 0;
    size_ = 0;
}

void audio_buffer::push(const float* data, std::size_t count) {
    if (data == nullptr || count == 0) {
        return;
    }
    // Ensure the data stays contiguous at the front. Compact first, then grow.
    if (head_ + size_ + count > data_.size()) {
        if (count <= data_.size()) {
            _compact();
        }
        if (size_ + count > data_.size()) {
            data_.resize(size_ + count);
        }
    }
    std::memcpy(data_.data() + head_ + size_, data, sizeof(float) * count);
    size_ += count;
}

std::size_t audio_buffer::pop(float* out, std::size_t count) {
    const std::size_t n = std::min(count, size_);
    if (n == 0) {
        return 0;
    }
    if (out != nullptr) {
        std::memcpy(out, data_.data() + head_, sizeof(float) * n);
    }
    head_ += n;
    size_ -= n;
    if (size_ == 0) {
        head_ = 0;
    } else if (head_ > data_.size() / 2) {
        _compact();
    }
    return n;
}

std::size_t audio_buffer::discard(std::size_t count) {
    return pop(nullptr, count);
}

const float* audio_buffer::data() const {
    _compact();
    return data_.data();
}

std::size_t audio_buffer::peek(float* out, std::size_t offset, std::size_t count) const {
    if (offset >= size_) {
        return 0;
    }
    const std::size_t n = std::min(count, size_ - offset);
    if (out != nullptr) {
        std::memcpy(out, data_.data() + head_ + offset, sizeof(float) * n);
    }
    return n;
}

void audio_buffer::_compact() const {
    if (head_ == 0 || size_ == 0) {
        head_ = 0;
        return;
    }
    if (head_ + size_ > data_.size()) {
        // Defensive: with a valid invariant this cannot happen, but keep the buffer usable.
        data_.resize(head_ + size_);
    }
    std::memmove(data_.data(), data_.data() + head_, sizeof(float) * size_);
    head_ = 0;
}

}  // namespace dfn::dsp
