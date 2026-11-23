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
#include <vector>

namespace dfn::dsp {

class Resampler {
   public:
    Resampler(int input_rate, int output_rate);

    int input_rate() const { return input_rate_; }
    int output_rate() const { return output_rate_; }
    bool is_identity() const { return input_rate_ == output_rate_; }

    void reset();

    // Upper bound on the number of samples `process` can write for `input_count` inputs.
    std::size_t max_output_samples(std::size_t input_count) const;

    // Number of output samples produced for `input_count` inputs (rounded to nearest).
    std::size_t output_length(std::size_t input_count) const;

    // Streams `count` input samples, writing as many complete output samples as possible.
    // Returns the number of output samples written (never exceeds `out_capacity`).
    std::size_t process(const float* input, std::size_t count, float* out,
                        std::size_t out_capacity);

    // Emits the remaining output samples, treating input beyond the end as silence.
    // `total_output` is the sample count returned by output_length() for all pushed input.
    std::size_t flush(float* out, std::size_t out_capacity);

    // Number of samples flushed so far (for bookkeeping in callers).
    std::size_t total_input() const { return total_input_; }

   private:
    float _sample_at(std::int64_t index) const;
    float _interpolate(double position) const;
    void _trim_history(std::int64_t next_position);

    int input_rate_;
    int output_rate_;
    double ratio_;  // input_rate / output_rate

    std::vector<float> history_;
    std::size_t history_head_ = 0;
    std::int64_t history_start_ = 0;
    std::size_t total_input_ = 0;
    std::uint64_t next_output_ = 0;
};

}  // namespace dfn::dsp

#endif  // DFN_DSP_RESAMPLER_H_
