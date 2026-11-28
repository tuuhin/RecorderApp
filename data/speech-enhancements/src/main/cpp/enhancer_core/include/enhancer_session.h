#ifndef DFN_ENHANCER_ENHANCER_SESSION_H_
#define DFN_ENHANCER_ENHANCER_SESSION_H_

// Streaming enhancement session: accepts PCM16 or float32 audio in arbitrary chunk sizes
// and emits enhanced audio in lockstep, delayed by `latency_samples()`.
//
//   session.process(chunk, out);   // any number of samples, any chunking
//   session.flush(out);            // pads the tail and drains the algorithmic latency
//
// The concatenation of all emitted samples for a stream is independent of how the input was
// chunked, and `flush()` always produces exactly `input_samples` (rounded up to a hop) of
// usable output once the leading `latency_samples()` are dropped.

#include <cstddef>
#include <cstdint>
#include <deque>
#include <memory>
#include <span>
#include <vector>

#include "audio_frame.h"
#include "deepfilter_engine.h"
#include "enhancer_config.h"
#include "model_config.h"
#include "onnx_backend.h"

namespace dfn::enhancer {

class EnhancerSession {
   public:
    struct Stats {
        std::uint64_t input_samples = 0;
        std::uint64_t output_samples = 0;
        std::uint64_t frames_processed = 0;
        float last_lsnr = 0.0f;
        EnhancementStage last_stage = EnhancementStage::kBypass;
    };

    EnhancerSession(std::shared_ptr<model_runner> runner, model_config config,
                    EnhancerOptions options = {});

    // Processes float samples; appends the produced samples to `output`.
    // Returns the number of samples appended.
    std::size_t process(std::span<const float> input, std::vector<float>& output);
    std::size_t process(const float* input, std::size_t count, std::vector<float>& output) {
        return process(std::span{input, count}, output);
    }

    // PCM16 variants of `process`.
    std::size_t process_pcm16(std::span<const std::int16_t> input,
                              std::vector<std::int16_t>& output);
    std::size_t process_pcm16(const std::int16_t* input, std::size_t count,
                              std::vector<std::int16_t>& output) {
        return process_pcm16(std::span{input, count}, output);
    }

    // Completes the stream: pads a partial hop and pushes the model/delay tail through the
    // pipeline. Appends the remaining samples to `output` and returns their count.
    std::size_t flush(std::vector<float>& output);
    std::size_t flush_pcm16(std::vector<std::int16_t>& output);

    void reset();

    [[nodiscard]] int latency_samples() const noexcept {
        return engine_.config().latency_samples();
    }
    [[nodiscard]] constexpr const Stats& stats() const noexcept { return stats_; }
    [[nodiscard]] const model_config& config() const noexcept { return engine_.config(); }
    [[nodiscard]] DeepFilterEngine& engine() noexcept { return engine_; }
    [[nodiscard]] const DeepFilterEngine& engine() const noexcept { return engine_; }
    [[nodiscard]] const std::vector<float>& frame_lsnrs() const noexcept { return frame_lsnrs_; }

   private:
    void append_frame(std::span<const float> frame, std::vector<float>& output);

    DeepFilterEngine engine_;
    dsp::frame_splitter splitter_;
    std::deque<float> pending_frames_;
    std::vector<float> frame_lsnrs_;
    Stats stats_;
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_ENHANCER_SESSION_H_
