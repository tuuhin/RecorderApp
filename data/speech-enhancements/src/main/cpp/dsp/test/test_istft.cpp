#include "istft.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <complex>
#include <vector>

#include "stft.h"
#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr int kSampleRate = 48000;
constexpr double kPi = 3.14159265358979323846;

StftConfig default_config() {
    return StftConfig{960, 480};
}

std::vector<float> make_sine(int sample_count, double frequency, float amplitude = 0.5f) {
    std::vector<float> signal(static_cast<std::size_t>(sample_count));
    for (std::size_t i = 0; i < signal.size(); ++i) {
        signal[i] = amplitude * static_cast<float>(std::sin(2.0 * kPi * frequency *
                                                            static_cast<double>(i) / kSampleRate));
    }
    return signal;
}

TEST(Istft, ZeroSpectrumProducesZeroSignal) {
    Istft istft(default_config());
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(istft.n_freqs()),
                                              {0.0f, 0.0f});
    std::vector<float> frame(480);
    for (int f = 0; f < 4; ++f) {
        istft.process(spectrum.data(), frame.data());
        for (int i = 0; i < 480; ++i) {
            EXPECT_EQ(frame[static_cast<std::size_t>(i)], 0.0f);
        }
    }
}

TEST(Istft, ReconstructsOriginalSignalWithStftRoundTrip) {
    StftConfig config = default_config();
    Stft stft(config);
    Istft istft(config);

    const std::vector<float> signal = make_sine(480 * 8, 440.0, 0.4f);
    const std::vector<std::complex<float>> spectrum = stft.analyze(signal.data(), signal.size());
    const std::vector<float> reconstructed = istft.synthesize(spectrum.data(), 8);

    // The analysis/synthesis loop delays the signal by fft_size - hop_size samples.
    const int delay = stft.fft_size() - stft.hop_size();
    const std::size_t compared = reconstructed.size() - static_cast<std::size_t>(delay);
    std::vector<float> expected(compared);
    std::copy(
        signal.begin(), signal.begin() + static_cast<std::ptrdiff_t>(compared), expected.begin());
    std::vector<float> actual(compared);
    std::copy(reconstructed.begin() + delay, reconstructed.end(), actual.begin());

    const test::Metrics metrics = test::compute_metrics(expected.data(), actual.data(), compared);
    test::expect_max_abs("stft -> istft reconstruction (440 Hz, 3840 samples)", metrics, 1e-5f);
}

TEST(Istft, MatchesUpstreamReferenceReconstruction) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<std::complex<float>> spectrum = test::load_complex("stft_spec");
    const std::vector<float> expected = test::load_f32("istft_out");
    const std::vector<int> shape = test::load_shape("stft_spec");
    ASSERT_EQ(shape.size(), 2u);
    const int frames = shape[0];

    Istft istft(default_config());
    const std::vector<float> actual = istft.synthesize(spectrum.data(), frames);
    ASSERT_EQ(actual.size(), expected.size());

    const test::Metrics metrics =
        test::compute_metrics(expected.data(), actual.data(), expected.size());
    test::expect_max_abs("istft reconstruction [2880] float32", metrics, 1e-5f);
}

TEST(Istft, OverlapAddIsContinuousForASine) {
    StftConfig config = default_config();
    Stft stft(config);
    Istft istft(config);
    const std::vector<float> signal = make_sine(480 * 6, 1000.0, 0.5f);
    const std::vector<std::complex<float>> spectrum = stft.analyze(signal.data(), signal.size());
    const std::vector<float> reconstructed = istft.synthesize(spectrum.data(), 6);

    const int delay = 480;
    // Compare the derivative (sample-to-sample difference) after the initial delay against
    // the original signal; a broken overlap-add shows up as jumps at hop boundaries.
    float max_step = 0.0f;
    for (std::size_t i = static_cast<std::size_t>(delay) + 1; i < reconstructed.size(); ++i) {
        const float expected_step = signal[i - static_cast<std::size_t>(delay)] -
                                    signal[i - 1 - static_cast<std::size_t>(delay)];
        const float actual_step = reconstructed[i] - reconstructed[i - 1];
        max_step = std::max(max_step, std::abs(actual_step - expected_step));
    }
    EXPECT_LT(max_step, 1e-4f);
}

TEST(Istft, BoundaryHandlingLeavesStartOfStreamFreeOfSignals) {
    // With a fresh state, the first hop cannot contain any past energy.
    Istft istft(default_config());
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(istft.n_freqs()),
                                              {0.0f, 0.0f});
    // A DC-only spectrum produces a windowed constant frame; the first hop is window-start
    // weighted, which is close to zero at the very first sample.
    spectrum[0] = std::complex<float>(1.0f, 0.0f);
    std::vector<float> frame(480);
    istft.process(spectrum.data(), frame.data());
    EXPECT_LT(std::abs(frame.front()), 1e-3f);
}

TEST(Istft, ResetClearsOverlapState) {
    StftConfig config = default_config();
    Stft stft(config);
    Istft istft(config);
    const std::vector<float> signal = make_sine(480 * 4, 440.0, 0.5f);
    const std::vector<std::complex<float>> spectrum = stft.analyze(signal.data(), signal.size());

    const std::vector<float> first = istft.synthesize(spectrum.data(), 4);
    istft.reset();
    const std::vector<float> second = istft.synthesize(spectrum.data(), 4);
    EXPECT_EQ(first, second);
}

}  // namespace
}  // namespace dfn::dsp
