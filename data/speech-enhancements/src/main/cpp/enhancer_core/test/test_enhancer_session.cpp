#include "enhancer_session.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <memory>
#include <vector>

#include "support/stub_runner.h"

namespace dfn::enhancer {
namespace {

model_config test_config() {
    model_config config = model_config::deepfilternet3_default();
    return config;
}

std::shared_ptr<test::StubRunner> make_runner(const model_config& config) {
    return std::make_shared<test::StubRunner>(config);
}

std::vector<float> make_signal(std::size_t count, float amplitude = 0.3f) {
    std::vector<float> signal(count);
    for (std::size_t i = 0; i < count; ++i) {
        signal[i] = amplitude * std::sin(0.01f * static_cast<float>(i));
    }
    return signal;
}

std::vector<float> run_stream(const std::vector<float>& input, std::size_t chunk) {
    const model_config config = test_config();
    EnhancerSession session(make_runner(config), config, EnhancerOptions{});
    std::vector<float> output;
    output.reserve(input.size() + 4096);
    for (std::size_t offset = 0; offset < input.size(); offset += chunk) {
        const std::size_t count = std::min(chunk, input.size() - offset);
        session.process(input.data() + offset, count, output);
    }
    session.flush(output);
    return output;
}

TEST(EnhancerSession, ReportsAlgorithmicLatency) {
    const model_config config = test_config();
    EnhancerSession session(make_runner(config), config);
    EXPECT_EQ(session.latency_samples(), 1440);
    EXPECT_EQ(session.latency_samples(), 2 * config.hop_size + (config.fft_size - config.hop_size));
}

TEST(EnhancerSession, OutputIsIndependentOfChunking) {
    const std::vector<float> input = make_signal(48000);
    const std::vector<float> reference = run_stream(input, input.size());
    ASSERT_FALSE(reference.empty());

    for (const std::size_t chunk : {1u, 17u, 479u, 480u, 481u, 4096u, 20000u}) {
        const std::vector<float> actual = run_stream(input, chunk);
        ASSERT_EQ(actual.size(), reference.size()) << "chunk " << chunk;
        for (std::size_t i = 0; i < actual.size(); ++i) {
            ASSERT_FLOAT_EQ(actual[i], reference[i]) << "chunk " << chunk << " index " << i;
        }
    }
}

TEST(EnhancerSession, FlushDrainsTheLatencyTail) {
    const model_config config = test_config();
    const std::size_t hop = static_cast<std::size_t>(config.hop_size);
    const std::size_t samples = 10 * hop;

    EnhancerSession session(make_runner(config), config);
    std::vector<float> output;
    session.process(make_signal(samples).data(), samples, output);
    EXPECT_EQ(output.size(), samples);  // lockstep, includes the leading delay
    session.flush(output);
    EXPECT_EQ(output.size(), samples + static_cast<std::size_t>(session.latency_samples()));
}

TEST(EnhancerSession, PartialFinalHopIsPaddedOnFlush) {
    const model_config config = test_config();
    const std::size_t hop = static_cast<std::size_t>(config.hop_size);
    EnhancerSession session(make_runner(config), config);
    const std::vector<float> input = make_signal(hop + 100);
    std::vector<float> output;
    session.process(input.data(), input.size(), output);
    EXPECT_EQ(output.size(), hop);
    session.flush(output);
    // One padded hop plus the latency tail.
    EXPECT_EQ(output.size(), 2 * hop + static_cast<std::size_t>(session.latency_samples()));
}

TEST(EnhancerSession, ResetRestoresInitialState) {
    const std::vector<float> input = make_signal(9600);
    const model_config config = test_config();
    EnhancerSession session(make_runner(config), config);

    std::vector<float> first;
    session.process(input.data(), input.size(), first);
    session.flush(first);

    session.reset();
    std::vector<float> second;
    session.process(input.data(), input.size(), second);
    session.flush(second);

    ASSERT_EQ(first.size(), second.size());
    for (std::size_t i = 0; i < first.size(); ++i) {
        ASSERT_FLOAT_EQ(first[i], second[i]) << "index " << i;
    }
}

TEST(EnhancerSession, Pcm16MatchesFloatPipelineQuantized) {
    const model_config config = test_config();
    const std::size_t hop = static_cast<std::size_t>(config.hop_size);

    std::vector<float> signal = make_signal(4 * hop, 0.5f);
    std::vector<int16_t> pcm(signal.size());
    std::vector<float> dequantized(signal.size());
    for (std::size_t i = 0; i < signal.size(); ++i) {
        pcm[i] = static_cast<int16_t>(signal[i] * 32768.0f);
        dequantized[i] = static_cast<float>(pcm[i]) / 32768.0f;
    }

    EnhancerSession float_session(make_runner(config), config);
    std::vector<float> float_output;
    float_session.process(dequantized.data(), dequantized.size(), float_output);
    float_session.flush(float_output);

    EnhancerSession pcm_session(make_runner(config), config);
    std::vector<int16_t> pcm_output;
    pcm_session.process_pcm16(pcm.data(), pcm.size(), pcm_output);
    pcm_session.flush_pcm16(pcm_output);

    ASSERT_EQ(pcm_output.size(), float_output.size());
    for (std::size_t i = 0; i < pcm_output.size(); ++i) {
        const float expected = float_output[i] * 32768.0f;
        const float clamped = std::min(std::max(expected, -32768.0f), 32767.0f);
        ASSERT_EQ(pcm_output[i], static_cast<int16_t>(clamped)) << "index " << i;
    }
}

TEST(EnhancerSession, StatsTrackProcessedFrames) {
    const model_config config = test_config();
    const std::size_t hop = static_cast<std::size_t>(config.hop_size);
    EnhancerSession session(make_runner(config), config);
    const std::vector<float> input = make_signal(8 * hop);
    std::vector<float> output;
    session.process(input.data(), input.size(), output);
    EXPECT_EQ(session.stats().input_samples, 8 * hop);
    EXPECT_EQ(session.stats().frames_processed, 8u);
    EXPECT_EQ(session.stats().output_samples, 8 * hop);
    EXPECT_EQ(output.size(), session.stats().output_samples);
}

}  // namespace
}  // namespace dfn::enhancer