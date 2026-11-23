#include "audio_frame.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <limits>
#include <vector>

namespace dfn::dsp {
namespace {

TEST(Pcm16Conversion, MinAndMaxSamples) {
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(std::numeric_limits<int16_t>::min()), -1.0f);
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(std::numeric_limits<int16_t>::max()),
                    static_cast<float>(32767) / 32768.0f);
}

TEST(Pcm16Conversion, Zero) {
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(0), 0.0f);
}

TEST(Pcm16Conversion, PositiveAndNegativeValues) {
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(16384), 0.5f);
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(-16384), -0.5f);
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(1), 1.0f / 32768.0f);
    EXPECT_FLOAT_EQ(pcm16_sample_to_float(-1), -1.0f / 32768.0f);
}

TEST(Pcm16Conversion, FloatToInt16) {
    EXPECT_EQ(float_sample_to_pcm16(0.0f), 0);
    EXPECT_EQ(float_sample_to_pcm16(0.5f), 16384);
    EXPECT_EQ(float_sample_to_pcm16(-0.5f), -16384);
    EXPECT_EQ(float_sample_to_pcm16(1.0f), 32767);
    EXPECT_EQ(float_sample_to_pcm16(-1.0f), -32768);
}

TEST(Pcm16Conversion, SaturatesOutOfRangeInput) {
    EXPECT_EQ(float_sample_to_pcm16(2.0f), 32767);
    EXPECT_EQ(float_sample_to_pcm16(-2.0f), -32768);
    EXPECT_EQ(float_sample_to_pcm16(1.5f * 32768.0f), 32767);
    EXPECT_EQ(float_sample_to_pcm16(-1.5f * 32768.0f), -32768);
    EXPECT_EQ(float_sample_to_pcm16(std::numeric_limits<float>::infinity()), 32767);
    EXPECT_EQ(float_sample_to_pcm16(-std::numeric_limits<float>::infinity()), -32768);
    EXPECT_EQ(float_sample_to_pcm16(std::numeric_limits<float>::quiet_NaN()), 0);
}

TEST(Pcm16Conversion, RoundTripsEveryInt16Value) {
    for (int value = -32768; value <= 32767; ++value) {
        const auto sample = static_cast<int16_t>(value);
        const int16_t round_trip = float_sample_to_pcm16(pcm16_sample_to_float(sample));
        ASSERT_EQ(round_trip, sample) << "value=" << value;
    }
}

TEST(Pcm16Conversion, BufferConversionMatchesScalarPath) {
    std::vector<int16_t> pcm = {0, 1, -1, 16384, -16384, 32767, -32768};
    const std::vector<float> as_float = pcm16_to_float(pcm);
    ASSERT_EQ(as_float.size(), pcm.size());
    for (std::size_t i = 0; i < pcm.size(); ++i) {
        EXPECT_FLOAT_EQ(as_float[i], pcm16_sample_to_float(pcm[i]));
    }
    const std::vector<int16_t> back = float_to_pcm16(as_float);
    EXPECT_EQ(back, pcm);
}

TEST(frame_splitter, ExactFrameBoundary) {
    frame_splitter splitter(480);
    std::vector<float> input(480);
    for (int i = 0; i < 480; ++i) {
        input[static_cast<std::size_t>(i)] = static_cast<float>(i);
    }
    std::vector<float> frames;
    EXPECT_EQ(splitter.push(input.data(), input.size(), frames), 1u);
    EXPECT_EQ(frames, input);
    EXPECT_EQ(splitter.pending(), 0u);
}

TEST(frame_splitter, OverlappingFramesDoNotOverlapOrDuplicateSamples) {
    // "Overlapping" in the DSP sense: consecutive frames cover consecutive, disjoint hops.
    frame_splitter splitter(480);
    std::vector<float> signal(480 * 5 + 123);
    for (std::size_t i = 0; i < signal.size(); ++i) {
        signal[i] = static_cast<float>(i);
    }
    std::vector<float> frames;
    const std::size_t produced = splitter.push(signal.data(), signal.size(), frames);
    EXPECT_EQ(produced, 5u);
    ASSERT_EQ(frames.size(), 480u * 5);

    // Reconstructed stream equals the original prefix exactly.
    for (std::size_t i = 0; i < frames.size(); ++i) {
        ASSERT_FLOAT_EQ(frames[i], signal[i]) << "index " << i;
    }
    EXPECT_EQ(splitter.pending(), 123u);
}

TEST(frame_splitter, PartialInputFramesAreBuffered) {
    frame_splitter splitter(100);
    std::vector<float> first(70, 1.0f);
    std::vector<float> frames;
    EXPECT_EQ(splitter.push(first.data(), first.size(), frames), 0u);
    EXPECT_EQ(splitter.pending(), 70u);

    std::vector<float> second(50, 2.0f);
    EXPECT_EQ(splitter.push(second.data(), second.size(), frames), 1u);
    ASSERT_EQ(frames.size(), 100u);
    for (int i = 0; i < 70; ++i) {
        EXPECT_FLOAT_EQ(frames[static_cast<std::size_t>(i)], 1.0f);
    }
    for (int i = 70; i < 100; ++i) {
        EXPECT_FLOAT_EQ(frames[static_cast<std::size_t>(i)], 2.0f);
    }
    EXPECT_EQ(splitter.pending(), 20u);
}

TEST(frame_splitter, IncompleteFinalFrameIsZeroPaddedOnFlush) {
    frame_splitter splitter(100);
    std::vector<float> input(130, 0.25f);
    std::vector<float> frames;
    EXPECT_EQ(splitter.push(input.data(), input.size(), frames), 1u);
    EXPECT_TRUE(splitter.flush(frames));
    ASSERT_EQ(frames.size(), 200u);
    for (int i = 100; i < 130; ++i) {
        EXPECT_FLOAT_EQ(frames[static_cast<std::size_t>(i)], 0.25f);
    }
    for (int i = 130; i < 200; ++i) {
        EXPECT_FLOAT_EQ(frames[static_cast<std::size_t>(i)], 0.0f);
    }
    EXPECT_FALSE(splitter.flush(frames));
}

TEST(frame_splitter, ArbitraryChunkSizesProduceIdenticalFrames) {
    std::vector<float> signal(2000);
    for (std::size_t i = 0; i < signal.size(); ++i) {
        signal[i] = static_cast<float>(i) * 0.001f;
    }
    frame_splitter whole(480);
    std::vector<float> whole_frames;
    whole.push(signal.data(), signal.size(), whole_frames);

    const std::size_t chunk_sizes[] = {1, 7, 64, 480, 513};
    for (const std::size_t chunk : chunk_sizes) {
        frame_splitter splitter(480);
        std::vector<float> frames;
        for (std::size_t offset = 0; offset < signal.size(); offset += chunk) {
            const std::size_t count = std::min(chunk, signal.size() - offset);
            splitter.push(signal.data() + offset, count, frames);
        }
        EXPECT_EQ(frames, whole_frames) << "chunk size " << chunk;
        EXPECT_EQ(splitter.pending(), whole.pending()) << "chunk size " << chunk;
    }
}

TEST(frame_splitter, NoSamplesSkippedOrDuplicatedAtBoundaries) {
    frame_splitter splitter(64);
    std::vector<float> frames;
    std::vector<float> collected;
    for (std::size_t i = 0; i < 1000; ++i) {
        const float sample = static_cast<float>(i);
        splitter.push(&sample, 1, frames);
        collected.insert(collected.end(), frames.begin(), frames.end());
        frames.clear();
    }
    ASSERT_EQ(collected.size(), 960u);
    for (std::size_t i = 0; i < collected.size(); ++i) {
        ASSERT_FLOAT_EQ(collected[i], static_cast<float>(i)) << "index " << i;
    }
    EXPECT_EQ(splitter.pending(), 40u);
}

}  // namespace
}  // namespace dfn::dsp
