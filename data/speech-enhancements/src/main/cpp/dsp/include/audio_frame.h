#ifndef DFN_DSP_AUDIO_FRAME_H_
#define DFN_DSP_AUDIO_FRAME_H_

// PCM conversion and hop-size framing.
//
// Public audio format is signed PCM16; all internal processing is float32 in [-1, 1).
// The conversion constants follow DeepFilterNet's DeepFilterNet/df/io.py:
//   int16 -> float : sample / 32768
//   float -> int16 : sample * 32768, truncated toward zero
// One documented deviation: out-of-range float input saturates instead of wrapping around
// (torch's int16 cast wraps, which is not desirable for a playback/recording pipeline).

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

namespace dfn::dsp {

inline constexpr float kPcm16Scale = 32768.0f;
inline constexpr float kPcm16InvScale = 1.0f / kPcm16Scale;

// Converts a single PCM16 sample to normalized float32.
[[nodiscard]] constexpr float pcm16_sample_to_float(std::int16_t sample) noexcept {
    return static_cast<float>(sample) * kPcm16InvScale;
}

// Converts a single normalized float32 sample to PCM16 with rounding toward zero and
// saturation to the int16 range.
[[nodiscard]] std::int16_t float_sample_to_pcm16(float sample) noexcept;

void pcm16_to_float(std::span<const std::int16_t> input, std::span<float> output);
void float_to_pcm16(std::span<const float> input, std::span<std::int16_t> output);

[[nodiscard]] std::vector<float> pcm16_to_float(std::span<const std::int16_t> input);
[[nodiscard]] std::vector<std::int16_t> float_to_pcm16(std::span<const float> input);

// Splits a sample stream into non-overlapping frames of exactly `hop_size` samples.
// Samples are never duplicated, reordered or dropped, even when input arrives in
// arbitrary chunk sizes.
class frame_splitter {
   public:
    explicit frame_splitter(int hop_size) noexcept;

    [[nodiscard]] constexpr int hop_size() const noexcept { return hop_size_; }
    void reset() noexcept;
    [[nodiscard]] constexpr std::size_t pending() const noexcept { return pending_.size(); }

    // Appends every complete frame contained in `input` to `frames` (frame-major, exactly
    // hop_size() samples per frame). Returns the number of frames appended.
    std::size_t push(std::span<const float> input, std::vector<float>& frames);

    // Emits one final frame when samples remain, zero-padding the tail. Returns false when
    // there was nothing left to flush.
    bool flush(std::vector<float>& frames);

   private:
    int hop_size_ = 1;
    std::vector<float> pending_;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_AUDIO_FRAME_H_
