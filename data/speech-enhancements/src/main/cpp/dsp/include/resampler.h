#ifndef DFN_DSP_RESAMPLER_H_
#define DFN_DSP_RESAMPLER_H_

// Streaming band-limited resampler.
//
// DeepFilterNet processes at 48 kHz. Input audio frequently arrives at 16 kHz or 44.1 kHz,
// so the DSP layer provides a streaming resampler that can be applied before enhancement and
// mirrored on the output path.
//
// The implementation is a windowed-sinc (Kaiser) interpolator with per-output weight
// normalization, which guarantees unity DC gain and identical results regardless of how the
// input is chunked. No external resampling library is required: the polyphase evaluation is
// a hand-written 2*16-tap kernel, which keeps the dependency surface minimal.

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

namespace dfn::dsp {

class Resampler {
   public:
    Resampler(int input_rate, int output_rate);

    [[nodiscard]] constexpr int input_rate() const noexcept { return input_rate_; }
    [[nodiscard]] constexpr int output_rate() const noexcept { return output_rate_; }
    [[nodiscard]] constexpr bool is_identity() const noexcept {
        return input_rate_ == output_rate_;
    }

    void reset() noexcept;

    // Upper bound on the number of samples `process` can write for `input_count` inputs.
    [[nodiscard]] std::size_t max_output_samples(std::size_t input_count) const;

    // Number of output samples produced for `input_count` inputs (rounded to nearest).
    [[nodiscard]] std::size_t output_length(std::size_t input_count) const;

    // Streams `input` samples, writing as many complete output samples as possible into `out`.
    // Returns the number of output samples written (never exceeds `out.size()`).
    std::size_t process(std::span<const float> input, std::span<float> out);

    // Emits the remaining output samples, treating input beyond the end as silence.
    std::size_t flush(std::span<float> out);

    // Number of samples pushed so far (for bookkeeping in callers).
    [[nodiscard]] constexpr std::size_t total_input() const noexcept { return total_input_; }

   private:
    [[nodiscard]] float sample_at(std::int64_t index) const noexcept;
    [[nodiscard]] float interpolate(double position) const;
    void trim_history(std::int64_t next_position);

    int input_rate_ = 0;
    int output_rate_ = 0;
    double ratio_ = 1.0;  // input_rate / output_rate

    std::vector<float> history_;
    std::size_t history_head_ = 0;
    std::int64_t history_start_ = 0;
    std::size_t total_input_ = 0;
    std::uint64_t next_output_ = 0;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_RESAMPLER_H_
