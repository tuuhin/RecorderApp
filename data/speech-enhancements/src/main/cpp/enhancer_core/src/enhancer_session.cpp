#include "enhancer_session.h"

#include <algorithm>
#include <cstring>

namespace dfn::enhancer {
namespace {

// Number of extra hops pushed through the pipeline on flush: the model lookahead plus the
// STFT window delay, both expressed in hops.
int flush_hop_count(const ModelConfig& config) {
    return config.latency_samples() / config.hop_size;
}

}  // namespace

EnhancerSession::EnhancerSession(std::shared_ptr<ModelRunner> runner, ModelConfig config,
                                 EnhancerOptions options)
    : engine_(std::move(config), std::move(runner), options),
      splitter_(engine_.config().hop_size) {}

void EnhancerSession::reset() {
    engine_.reset();
    splitter_.reset();
    pending_frames_.clear();
    stats_ = Stats{};
}

void EnhancerSession::_append_frame(const float* frame, std::vector<float>& output) {
    const int hop = engine_.config().hop_size;
    std::vector<float> enhanced(static_cast<std::size_t>(hop));
    engine_.process_frame(frame, enhanced.data());
    output.insert(output.end(), enhanced.begin(), enhanced.end());
    stats_.output_samples += static_cast<std::uint64_t>(hop);
    ++stats_.frames_processed;
    stats_.last_lsnr = engine_.last_lsnr();
    stats_.last_stage = engine_.last_stage();
}

std::size_t EnhancerSession::process(const float* input, std::size_t count,
                                     std::vector<float>& output) {
    const std::size_t before = output.size();
    std::vector<float> frames;
    splitter_.push(input, count, frames);
    stats_.input_samples += count;

    const auto hop = static_cast<std::size_t>(engine_.config().hop_size);
    const std::size_t frame_count = frames.size() / hop;
    for (std::size_t f = 0; f < frame_count; ++f) {
        _append_frame(frames.data() + f * hop, output);
    }
    return output.size() - before;
}

std::size_t EnhancerSession::process_pcm16(const int16_t* input, std::size_t count,
                                           std::vector<int16_t>& output) {
    const std::size_t before = output.size();
    std::vector<float> converted = dsp::pcm16_to_float(std::span<const int16_t>(input, count));
    std::vector<float> enhanced;
    enhanced.reserve(converted.size());
    process(converted.data(), converted.size(), enhanced);
    const std::vector<int16_t> pcm = dsp::float_to_pcm16(std::span<const float>(enhanced));
    output.insert(output.end(), pcm.begin(), pcm.end());
    return output.size() - before;
}

std::size_t EnhancerSession::flush(std::vector<float>& output) {
    const std::size_t before = output.size();
    const auto hop = static_cast<std::size_t>(engine_.config().hop_size);

    // Pad a trailing partial hop, if any.
    std::vector<float> frames;
    if (splitter_.flush(frames)) {
        _append_frame(frames.data() + frames.size() - hop, output);
    }

    // Push the model lookahead / STFT tail through the pipeline.
    std::vector<float> zeros(hop, 0.0f);
    const int tail_hops = flush_hop_count(engine_.config());
    for (int i = 0; i < tail_hops; ++i) {
        _append_frame(zeros.data(), output);
    }
    return output.size() - before;
}

std::size_t EnhancerSession::flush_pcm16(std::vector<int16_t>& output) {
    const std::size_t before = output.size();
    std::vector<float> enhanced;
    flush(enhanced);
    const std::vector<int16_t> pcm = dsp::float_to_pcm16(std::span<const float>(enhanced));
    output.insert(output.end(), pcm.begin(), pcm.end());
    return output.size() - before;
}

}  // namespace dfn::enhancer
