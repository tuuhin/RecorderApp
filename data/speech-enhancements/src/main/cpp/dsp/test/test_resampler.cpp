#include "resampler.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <span>
#include <utility>
#include <vector>

#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr double kPi = 3.14159265358979323846;

std::vector<float> make_sine(int sample_count, double sample_rate, double frequency,
                             float amplitude = 0.5f) {
    std::vector<float> signal(static_cast<std::size_t>(sample_count));
    for (std::size_t i = 0; i < signal.size(); ++i) {
        signal[i] = amplitude * static_cast<float>(std::sin(2.0 * kPi * frequency *
                                                            static_cast<double>(i) / sample_rate));
    }
    return signal;
}

// Estimates the frequency of a sine from zero crossings.
double estimate_frequency(const std::vector<float>& signal, double sample_rate) {
    int crossings = 0;
    for (std::size_t i = 1; i < signal.size(); ++i) {
        if ((signal[i - 1] < 0.0f && signal[i] >= 0.0f)) {
            ++crossings;
        }
    }
    const double seconds = static_cast<double>(signal.size()) / sample_rate;
    return static_cast<double>(crossings) / seconds;
}

float rms(const std::vector<float>& signal) {
    double sum = 0.0;
    for (const float value : signal) {
        sum += static_cast<double>(value) * static_cast<double>(value);
    }
    return static_cast<float>(std::sqrt(sum / static_cast<double>(signal.size())));
}

std::vector<float> resample_all(Resampler& resampler, const std::vector<float>& input,
                                std::size_t chunk,
                                std::vector<std::size_t>* chunk_lengths = nullptr) {
    std::vector<float> output;
    output.reserve(input.size());
    std::vector<float> buffer;
    std::size_t offset = 0;
    while (offset < input.size()) {
        const std::size_t count = std::min(chunk, input.size() - offset);
        buffer.resize(resampler.max_output_samples(count));
        const std::size_t produced =
            resampler.process(std::span{input}.subspan(offset, count), buffer);
        output.insert(
            output.end(), buffer.begin(), buffer.begin() + static_cast<std::ptrdiff_t>(produced));
        offset += count;
    }
    buffer.resize(4096);
    const std::size_t produced = resampler.flush(buffer);
    output.insert(
        output.end(), buffer.begin(), buffer.begin() + static_cast<std::ptrdiff_t>(produced));
    if (chunk_lengths != nullptr) {
        chunk_lengths->push_back(output.size());
    }
    return output;
}

TEST(Resampler, FortyEightToFortyEightIsBitExact) {
    Resampler resampler(48000, 48000);
    EXPECT_TRUE(resampler.is_identity());
    const std::vector<float> input = make_sine(1000, 48000.0, 440.0);
    const std::vector<float> output = resample_all(resampler, input, 173);
    ASSERT_EQ(output.size(), input.size());
    for (std::size_t i = 0; i < input.size(); ++i) {
        ASSERT_FLOAT_EQ(output[i], input[i]) << "index " << i;
    }
}

TEST(Resampler, SixteenToFortyEightPreservesFrequencyAndAmplitude) {
    const std::vector<float> input = make_sine(16000, 16000.0, 440.0, 0.5f);
    Resampler resampler(16000, 48000);
    const std::vector<float> output = resample_all(resampler, input, 320);

    // Guard against edge transients in the measurement window.
    std::vector<float> trimmed(output.begin() + 2048, output.end() - 2048);
    EXPECT_NEAR(estimate_frequency(trimmed, 48000.0), 440.0, 2.0);
    // A 0.5 amplitude sine has an RMS of 0.5/sqrt(2) ~= 0.3536.
    EXPECT_NEAR(rms(trimmed), 0.3536f, 0.01f);
    EXPECT_NEAR(static_cast<double>(output.size()), 48000.0, 64.0);
}

TEST(Resampler, FortyFourPointOneToFortyEightPreservesFrequencyAndAmplitude) {
    const std::vector<float> input = make_sine(44100, 44100.0, 1000.0, 0.4f);
    Resampler resampler(44100, 48000);
    const std::vector<float> output = resample_all(resampler, input, 441);

    std::vector<float> trimmed(output.begin() + 4096, output.end() - 4096);
    EXPECT_NEAR(estimate_frequency(trimmed, 48000.0), 1000.0, 2.0);
    EXPECT_NEAR(rms(trimmed), 0.4f / std::sqrt(2.0f), 0.01f);
    EXPECT_NEAR(static_cast<double>(output.size()), 48000.0, 128.0);
}

TEST(Resampler, ChunkedAndOneShotAreIdentical) {
    const std::vector<float> input = make_sine(44100, 44100.0, 700.0, 0.3f);
    Resampler one_shot(44100, 48000);
    const std::vector<float> expected = resample_all(one_shot, input, input.size());

    for (const std::size_t chunk : {1u, 13u, 441u, 4096u}) {
        Resampler chunked(44100, 48000);
        const std::vector<float> actual = resample_all(chunked, input, chunk);
        ASSERT_EQ(actual.size(), expected.size()) << "chunk " << chunk;
        for (std::size_t i = 0; i < actual.size(); ++i) {
            ASSERT_FLOAT_EQ(actual[i], expected[i]) << "chunk " << chunk << " index " << i;
        }
    }
}

TEST(Resampler, DownsamplingToSixteenKilohertzKeepsLowFrequencyContent) {
    const std::vector<float> input = make_sine(48000, 48000.0, 440.0, 0.5f);
    Resampler resampler(48000, 16000);
    const std::vector<float> output = resample_all(resampler, input, 480);
    EXPECT_NEAR(static_cast<double>(output.size()), 16000.0, 64.0);
    std::vector<float> trimmed(output.begin() + 1024, output.end() - 1024);
    EXPECT_NEAR(estimate_frequency(trimmed, 16000.0), 440.0, 4.0);
    EXPECT_NEAR(rms(trimmed), 0.3536f, 0.02f);
}

TEST(Resampler, ResetRestoresInitialState) {
    const std::vector<float> input = make_sine(4800, 48000.0, 440.0);
    Resampler resampler(16000, 48000);
    const std::vector<float> first = resample_all(resampler, input, 480);
    resampler.reset();
    const std::vector<float> second = resample_all(resampler, input, 480);
    ASSERT_EQ(first.size(), second.size());
    for (std::size_t i = 0; i < first.size(); ++i) {
        ASSERT_FLOAT_EQ(first[i], second[i]) << "index " << i;
    }
}

TEST(Resampler, OutputLengthMatchesDeclaredEstimate) {
    for (const auto [in_rate, out_rate] : {std::pair<int, int>{16000, 48000},
                                           std::pair<int, int>{44100, 48000},
                                           std::pair<int, int>{48000, 48000}}) {
        Resampler resampler(in_rate, out_rate);
        const std::size_t count = 12345;
        const std::vector<float> input(static_cast<std::size_t>(count), 0.0f);
        const std::vector<float> output = resample_all(resampler, input, count);
        EXPECT_EQ(output.size(), resampler.output_length(count)) << in_rate << " -> " << out_rate;
    }
}

}  // namespace
}  // namespace dfn::dsp
