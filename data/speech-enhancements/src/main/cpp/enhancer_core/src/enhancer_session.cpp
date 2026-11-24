#include "enhancer_session.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <ranges>
#include <span>
#include <utility>
#include <vector>

#include "audio_frame.h"

namespace dfn::enhancer {
namespace {

// Number of extra hops pushed through the pipeline on flush: the model lookahead plus the
// STFT window delay, both expressed in hops.
[[nodiscard]] constexpr int flush_hop_count(const model_config& config) noexcept {
    return config.latency_samples() / config.hop_size;
}

}  // namespace

EnhancerSession::EnhancerSession(std::shared_ptr<model_runner> runner, model_config config,
                                 EnhancerOptions options)
    : engine_(std::move(config), std::move(runner), options),
      splitter_(engine_.config().hop_size) {}

void EnhancerSession::reset() {
    engine_.reset();
    splitter_.reset();
    pending_frames_.clear();
    stats_ = Stats{};
}

void EnhancerSession::append_frame(std::span<const float> frame, std::vector<float>& output) {
    const auto hop = static_cast<std::size_t>(engine_.config().hop_size);
    const auto in_frame = frame.first(hop);

    std::vector<float> enhanced(hop, 0.0f);
    engine_.process_frame(in_frame, enhanced);
    output.insert(output.end(), enhanced.begin(), enhanced.end());

    stats_.output_samples += static_cast<std::uint64_t>(hop);
    ++stats_.frames_processed;
    stats_.last_lsnr = engine_.last_lsnr();
    stats_.last_stage = engine_.last_stage();
}

std::size_t EnhancerSession::process(std::span<const float> input, std::vector<float>& output) {
    const std::size_t before = output.size();
    std::vector<float> frames;
    splitter_.push(input, frames);
    stats_.input_samples += input.size();

    const auto hop = static_cast<std::size_t>(engine_.config().hop_size);
    const std::size_t frame_count = frames.size() / hop;
    const auto frames_span = std::span{frames};

    for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, frame_count)) {
        append_frame(frames_span.subspan(f * hop, hop), output);
    }

    return output.size() - before;
}

std::size_t EnhancerSession::process_pcm16(std::span<const std::int16_t> input,
                                           std::vector<std::int16_t>& output) {
    const std::size_t before = output.size();
    const std::vector<float> converted = dsp::pcm16_to_float(input);

    std::vector<float> enhanced;
    enhanced.reserve(converted.size());
    process(converted, enhanced);

    const std::vector<std::int16_t> pcm = dsp::float_to_pcm16(enhanced);
    output.insert(output.end(), pcm.begin(), pcm.end());
    return output.size() - before;
}

std::size_t EnhancerSession::flush(std::vector<float>& output) {
    const std::size_t before = output.size();
    const auto hop = static_cast<std::size_t>(engine_.config().hop_size);

    // Pad a trailing partial hop, if any.
    std::vector<float> frames;
    if (splitter_.flush(frames)) {
        const auto frames_span = std::span{frames};
        append_frame(frames_span.subspan(frames_span.size() - hop, hop), output);
    }

    // Push the model lookahead / STFT tail through the pipeline.
    const std::vector<float> zeros(hop, 0.0f);
    const auto tail_hops = static_cast<std::size_t>(std::max(0, flush_hop_count(engine_.config())));
    for (const std::weakly_incrementable auto _ : std::views::iota(std::size_t{0}, tail_hops)) {
        (void)_;
        append_frame(zeros, output);
    }

    return output.size() - before;
}

std::size_t EnhancerSession::flush_pcm16(std::vector<std::int16_t>& output) {
    const std::size_t before = output.size();
    std::vector<float> enhanced;
    flush(enhanced);

    const std::vector<std::int16_t> pcm = dsp::float_to_pcm16(enhanced);
    output.insert(output.end(), pcm.begin(), pcm.end());
    return output.size() - before;
}

}  // namespace dfn::enhancer
