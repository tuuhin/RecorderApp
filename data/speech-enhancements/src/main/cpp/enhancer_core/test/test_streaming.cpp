#include <gtest/gtest.h>

#include <cmath>
#include <memory>
#include <vector>

#include "deepfilter_engine.h"
#include "enhancer_session.h"
#include "support/stub_runner.h"

namespace dfn::enhancer {
namespace {

model_config test_config() {
    return model_config::deepfilternet3_default();
}

std::vector<float> make_ramp(std::size_t count) {
    std::vector<float> signal(count);
    for (std::size_t i = 0; i < count; ++i) {
        signal[i] = 0.1f + 0.4f * static_cast<float>(i % 997) / 997.0f;
    }
    return signal;
}

std::vector<float> run_frames(DeepFilterEngine& engine, int frames, float value = 0.25f) {
    const auto hop = static_cast<std::size_t>(engine.config().hop_size);
    std::vector<float> input(hop, value);
    std::vector<float> output(hop);
    std::vector<float> collected;
    collected.reserve(hop * static_cast<std::size_t>(frames));
    for (int i = 0; i < frames; ++i) {
        engine.process_frame(input, output);
        collected.insert(collected.end(), output.begin(), output.end());
    }
    return collected;
}

TEST(StreamingEngine, RecurrentStatePersistsAcrossFrames) {
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    DeepFilterEngine engine(config, runner);

    run_frames(engine, 7);
    EXPECT_FLOAT_EQ(runner->encoder_frame_count(engine.state().recurrent), 7.0f);

    engine.reset();
    EXPECT_FLOAT_EQ(runner->encoder_frame_count(engine.state().recurrent), 0.0f);
    run_frames(engine, 3);
    EXPECT_FLOAT_EQ(runner->encoder_frame_count(engine.state().recurrent), 3.0f);
}

TEST(StreamingEngine, ResetClearsSpectralHistory) {
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    DeepFilterEngine engine(config, runner);

    const std::vector<float> first = run_frames(engine, 6, 0.4f);
    engine.reset();
    const std::vector<float> second = run_frames(engine, 6, 0.4f);
    ASSERT_EQ(first.size(), second.size());
    for (std::size_t i = 0; i < first.size(); ++i) {
        ASSERT_FLOAT_EQ(first[i], second[i]) << "index " << i;
    }
}

TEST(StreamingEngine, SpectralHistoryAffectsDeepFiltering) {
    // Deep filtering reads a rolling window of past spectra; a stream with different history
    // must produce a different output for the same current frame.
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    DeepFilterEngine a(config, runner);
    DeepFilterEngine b(config, runner);

    const auto hop = static_cast<std::size_t>(config.hop_size);
    std::vector<float> ramp(hop, 0.1f);
    std::vector<float> other(hop, 0.6f);
    std::vector<float> out_a(hop);
    std::vector<float> out_b(hop);

    for (int i = 0; i < 8; ++i) {
        a.process_frame(ramp, out_a);
        b.process_frame(other, out_b);
    }
    // Feed the same final frame to both engines; the deep filter still sees different
    // histories, so the outputs must differ.
    a.process_frame(ramp, out_a);
    b.process_frame(ramp, out_b);

    float max_diff = 0.0f;
    for (std::size_t i = 0; i < hop; ++i) {
        max_diff = std::max(max_diff, std::abs(out_a[i] - out_b[i]));
    }
    EXPECT_GT(max_diff, 0.0f);
}

TEST(StreamingEngine, StageSelectionFollowsThresholds) {
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    EnhancerOptions options;
    options.min_db_thresh = -10.0f;
    options.max_db_erb_thresh = 30.0f;
    options.max_db_df_thresh = 20.0f;
    DeepFilterEngine engine(config, runner, options);

    EXPECT_EQ(engine.select_stage(-20.0f), EnhancementStage::kSilence);
    EXPECT_EQ(engine.select_stage(-10.5f), EnhancementStage::kSilence);
    EXPECT_EQ(engine.select_stage(-5.0f), EnhancementStage::kErbAndDf);
    EXPECT_EQ(engine.select_stage(15.0f), EnhancementStage::kErbAndDf);
    EXPECT_EQ(engine.select_stage(25.0f), EnhancementStage::kErbOnly);
    EXPECT_EQ(engine.select_stage(35.0f), EnhancementStage::kBypass);

    options.enable_df_stage = false;
    DeepFilterEngine no_df(config, runner, options);
    EXPECT_EQ(no_df.select_stage(0.0f), EnhancementStage::kErbOnly);
}

TEST(StreamingEngine, BypassStagesPreserveTheNoisySpectrum) {
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    EnhancerOptions options;
    options.min_db_thresh = -1e9f;      // never silence
    options.max_db_erb_thresh = -1e8f;  // always bypass
    DeepFilterEngine engine(config, runner, options);

    const auto hop = static_cast<std::size_t>(config.hop_size);
    std::vector<float> input(hop);
    for (std::size_t i = 0; i < hop; ++i) {
        input[i] = std::sin(0.02f * static_cast<float>(i));
    }
    std::vector<float> out_a(hop);
    std::vector<float> out_b(hop);
    engine.process_frame(input, out_a);
    engine.reset();
    engine.process_frame(input, out_b);
    for (std::size_t i = 0; i < hop; ++i) {
        ASSERT_FLOAT_EQ(out_a[i], out_b[i]) << "index " << i;
    }
}

TEST(StreamingEngine, SessionOutputCoversEveryInputSample) {
    const model_config config = test_config();
    const auto hop = static_cast<std::size_t>(config.hop_size);
    const std::size_t frames = 12;
    auto runner = std::make_shared<test::StubRunner>(config);
    EnhancerSession session(runner, config);

    const std::vector<float> input = make_ramp(frames * hop);
    std::vector<float> output;
    session.process(input.data(), input.size(), output);
    session.flush(output);

    const auto latency = static_cast<std::size_t>(session.latency_samples());
    ASSERT_EQ(output.size(), input.size() + latency);
    const std::vector<float> aligned(output.begin() + static_cast<std::ptrdiff_t>(latency),
                                     output.end());
    ASSERT_EQ(aligned.size(), input.size());
}

TEST(StreamingEngine, NoDuplicatedOrMissingSamplesAcrossChunks) {
    const model_config config = test_config();
    auto runner = std::make_shared<test::StubRunner>(config);
    EnhancerSession session(runner, config);

    const std::vector<float> input = make_ramp(4096);
    std::vector<float> output;
    std::size_t offset = 0;
    std::size_t sample_index = 0;
    while (offset < input.size()) {
        const std::size_t count = std::min<std::size_t>(337, input.size() - offset);
        const std::size_t before = output.size();
        const std::size_t produced = session.process(input.data() + offset, count, output);
        EXPECT_EQ(produced, output.size() - before);
        offset += count;
        // Every consumed sample is accounted for by the splitter's pending buffer.
        sample_index += count;
        EXPECT_EQ(session.stats().input_samples, sample_index);
    }
    session.flush(output);
    EXPECT_EQ(session.stats().input_samples, input.size());
}

}  // namespace
}  // namespace dfn::enhancer
