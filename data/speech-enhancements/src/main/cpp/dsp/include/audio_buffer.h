#ifndef DFN_DSP_AUDIO_BUFFER_H_
#define DFN_DSP_AUDIO_BUFFER_H_

// Small contiguous ring buffer used to decouple arbitrary input chunk sizes from
// hop-size processing in the streaming engine.

#include <cstddef>
#include <vector>

namespace dfn::dsp {

class audio_buffer {
   public:
    explicit audio_buffer(std::size_t capacity = 0);

    void reset();

    [[nodiscard]] std::size_t capacity() const { return data_.size(); }
    [[nodiscard]] std::size_t size() const { return size_; }
    [[nodiscard]] bool empty() const { return size_ == 0; }

    // Appends samples, growing the storage as required.
    void push(const float* data, std::size_t count);
    void push(const std::vector<float>& data) { push(data.data(), data.size()); }

    // Pops at most `count` samples into `out`. Returns the number of samples popped.
    std::size_t pop(float* out, std::size_t count);

    // Drops at most `count` leading samples. Returns the number dropped.
    std::size_t discard(std::size_t count);

    // Returns a pointer to the current contents, compacting the ring first when needed.
    [[nodiscard]] const float* data() const;

    // Reads up to `count` samples starting at `offset` without removing them.
    std::size_t peek(float* out, std::size_t offset, std::size_t count) const;

   private:
    void _compact() const;

    mutable std::vector<float> data_;
    mutable std::size_t head_ = 0;
    std::size_t size_ = 0;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_AUDIO_BUFFER_H_
