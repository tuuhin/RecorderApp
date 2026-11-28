#ifndef DFN_DSP_AUDIO_BUFFER_H_
#define DFN_DSP_AUDIO_BUFFER_H_

// Small contiguous buffer with compaction used to decouple arbitrary input chunk sizes
// from hop-size processing in the streaming engine.

#include <cstddef>
#include <span>
#include <vector>

namespace dfn::dsp {

class audio_buffer {
   public:
    explicit audio_buffer(std::size_t capacity = 0);

    void reset() noexcept;

    [[nodiscard]] constexpr std::size_t capacity() const noexcept { return data_.size(); }
    [[nodiscard]] constexpr std::size_t size() const noexcept { return size_; }
    [[nodiscard]] constexpr bool empty() const noexcept { return size_ == 0; }

    // Appends samples, growing the storage as required.
    void push(std::span<const float> data);

    // Pops at most `count` samples into `out`. Returns the number of samples popped.
    std::size_t pop(std::span<float> out, std::size_t count);
    std::size_t pop(std::span<float> out) { return pop(out, out.size()); }

    // Drops at most `count` leading samples. Returns the number dropped.
    std::size_t discard(std::size_t count);

    // Returns a view of the current contiguous contents, compacting first when needed.
    [[nodiscard]] std::span<const float> view() const;

    // Returns a pointer to the current contents, compacting the ring first when needed.
    [[nodiscard]] const float* data() const;

    // Reads up to `count` samples starting at `offset` without removing them.
    std::size_t peek(std::span<float> out, std::size_t offset, std::size_t count) const;

   private:
    void compact() const;

    mutable std::vector<float> data_;
    mutable std::size_t head_ = 0;
    std::size_t size_ = 0;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_AUDIO_BUFFER_H_
