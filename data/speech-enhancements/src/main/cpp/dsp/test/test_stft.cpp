#include "stft.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <complex>
#include <vector>

#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr int kSampleRate = 48000;
constexpr double kPi = 3.14159265358979323846;

stft_config default_config() {
    return stft_config{960, 480};
}

// Sine signal of the given frequency, one frame of hop_size samples.
std::vector<float> make_sine(int frame_count, double frequency, float amplitude = 0.5f) {
    std::vector<float> signal(static_cast<std::size_t>(frame_count * 480));
    for (std::size_t i = 0; i < signal.size(); ++i) {
        signal[i] = amplitude * static_cast<float>(std::sin(2.0 * kPi * frequency *
                                                            static_cast<double>(i) / kSampleRate));
    }
    return signal;
}

int peak_bin(const std::complex<float>* spectrum, int bins, int from = 1) {
    int peak = from;
    float best = std::abs(spectrum[from]);
    for (int k = from + 1; k < bins; ++k) {
        if (std::abs(spectrum[k]) > best) {
            best = std::abs(spectrum[k]);
            peak = k;
        }
    }
    return peak;
}

TEST(StftConfig, FrequencyBinCount) {
    EXPECT_EQ(freq_size(960), 481);
    EXPECT_EQ(freq_size(1024), 513);
}

TEST(StftConfig, RejectsInvalidConfigurations) {
    EXPECT_THROW(Stft(stft_config{960, 600}), std::invalid_argument);  // hop > N/2
    EXPECT_THROW(Stft(stft_config{960, 0}), std::invalid_argument);    // hop must be positive
    EXPECT_THROW(Stft(stft_config{100, 50}), std::invalid_argument);   // N not PFFFT-compatible
}

TEST(Stft, SilenceProducesExactZeroSpectrum) {
    Stft stft(default_config());
    const std::vector<float> silence(480, 0.0f);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(stft.n_freqs()));
    stft.process(silence.data(), spectrum.data());
    for (int k = 0; k < stft.n_freqs(); ++k) {
        EXPECT_EQ(spectrum[static_cast<std::size_t>(k)], std::complex<float>(0.0f, 0.0f))
            << "bin " << k;
    }
}

TEST(Stft, ImpulseAtWindowCentreHasFlatMagnitude) {
    // A unit impulse at the centre of the analysis window multiplies the window peak (1.0)
    // and therefore produces a flat spectrum of magnitude window_norm.
    Stft stft(default_config());
    std::vector<float> frame(480, 0.0f);
    // The second half of the window covers the current hop; the centre of the full window
    // lies at absolute index 480, which is the first sample of the second hop.
    frame[0] = 1.0f;
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(stft.n_freqs()));
    stft.process(frame.data(), spectrum.data());

    const float expected = stft.window()[480] * stft.window_norm();
    for (int k = 0; k < stft.n_freqs(); ++k) {
        ASSERT_NEAR(std::abs(spectrum[static_cast<std::size_t>(k)]), expected, 1e-6f)
            << "bin " << k;
    }
}

TEST(Stft, FftMatchesNaiveDft) {
    // Cross-checks the PFFFT packing/scale against the DFT definition used by rustfft.
    Stft stft(default_config());
    const std::vector<float> signal = make_sine(1, 1000.0, 0.3f);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(stft.n_freqs()));
    stft.process(signal.data(), spectrum.data());

    // Rebuild the exact analysis buffer (previous samples are zero on a fresh Stft).
    std::vector<float> windowed(960, 0.0f);
    for (int i = 0; i < 480; ++i) {
        windowed[static_cast<std::size_t>(i + 480)] =
            signal[static_cast<std::size_t>(i)] * stft.window()[static_cast<std::size_t>(i + 480)];
    }
    for (int k = 0; k < stft.n_freqs(); k += 17) {
        std::complex<double> reference(0.0, 0.0);
        for (int n = 0; n < 960; ++n) {
            const double angle =
                -2.0 * kPi * static_cast<double>(k) * static_cast<double>(n) / 960.0;
            reference += static_cast<double>(windowed[static_cast<std::size_t>(n)]) *
                         std::complex<double>(std::cos(angle), std::sin(angle));
        }
        reference *= static_cast<double>(stft.window_norm());
        EXPECT_NEAR(std::abs(spectrum[static_cast<std::size_t>(k)] -
                             std::complex<float>(static_cast<float>(reference.real()),
                                                 static_cast<float>(reference.imag()))),
                    0.0f,
                    1e-6f)
            << "bin " << k;
    }
}

TEST(Stft, FourHundredFourtyHertzSinePeaksAtExpectedBin) {
    Stft stft(default_config());
    const std::vector<float> signal = make_sine(3, 440.0);
    const std::vector<std::complex<float>> spectrum = stft.analyze(signal.data(), signal.size());
    const int bins = stft.n_freqs();
    // Bin width is 50 Hz, so 440 Hz falls between bins 8 and 9 and peaks at 9.
    EXPECT_EQ(peak_bin(spectrum.data() + bins, bins), 9);
    // A pure tone concentrates its energy in a small neighbourhood of the peak.
    const float peak = std::abs(spectrum[static_cast<std::size_t>(bins + 9)]);
    EXPECT_LT(std::abs(spectrum[static_cast<std::size_t>(bins + 40)]), peak * 0.01f);
}

TEST(Stft, OneKilohertzSinePeaksAtExpectedBin) {
    Stft stft(default_config());
    const std::vector<float> signal = make_sine(2, 1000.0);
    const std::vector<std::complex<float>> spectrum = stft.analyze(signal.data(), signal.size());
    EXPECT_EQ(peak_bin(spectrum.data() + stft.n_freqs(), stft.n_freqs()), 20);
}

TEST(Stft, MultipleSinesProduceMultiplePeaks) {
    Stft stft(default_config());
    const std::vector<float> a = make_sine(1, 440.0, 0.3f);
    const std::vector<float> b = make_sine(1, 1000.0, 0.3f);
    std::vector<float> mixed(a.size());
    for (std::size_t i = 0; i < a.size(); ++i) {
        mixed[i] = a[i] + b[i];
    }
    const std::vector<std::complex<float>> spectrum = stft.analyze(mixed.data(), mixed.size());
    EXPECT_GT(std::abs(spectrum[static_cast<std::size_t>(8)]), 0.02f);
    EXPECT_GT(std::abs(spectrum[static_cast<std::size_t>(9)]), 0.02f);
    EXPECT_GT(std::abs(spectrum[static_cast<std::size_t>(20)]), 0.02f);
    EXPECT_LT(std::abs(spectrum[static_cast<std::size_t>(60)]), 0.01f);
}

TEST(Stft, MatchesUpstreamReferenceSpectrogram) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<float> signal = test::load_f32("stft_signal");
    const std::vector<std::complex<float>> expected = test::load_complex("stft_spec");
    const std::vector<int> shape = test::load_shape("stft_spec");
    ASSERT_EQ(shape.size(), 2u);
    const int frames = shape[0];
    ASSERT_EQ(shape[1], 481);

    Stft stft(default_config());
    int frames_actual = 0;
    const std::vector<std::complex<float>> actual =
        stft.analyze(signal.data(), signal.size(), &frames_actual);
    ASSERT_EQ(frames_actual, frames);
    ASSERT_EQ(actual.size(), expected.size());

    const test::Metrics metrics =
        test::compute_metrics(expected.data(), actual.data(), expected.size());
    test::expect_max_abs("stft spectrogram [6,481] complex64", metrics, 1e-6f);
}

TEST(Stft, StreamingAndOneShotAnalysisAreIdentical) {
    const std::vector<float> signal = make_sine(6, 440.0);
    Stft one_shot(default_config());
    const std::vector<std::complex<float>> expected =
        one_shot.analyze(signal.data(), signal.size());

    Stft streaming(default_config());
    std::vector<std::complex<float>> actual;
    std::vector<std::complex<float>> frame(481);
    for (int f = 0; f < 6; ++f) {
        streaming.process(signal.data() + f * 480, frame.data());
        actual.insert(actual.end(), frame.begin(), frame.end());
    }
    ASSERT_EQ(actual.size(), expected.size());
    for (std::size_t i = 0; i < actual.size(); ++i) {
        ASSERT_EQ(actual[i], expected[i]) << "index " << i;
    }
}

}  // namespace
}  // namespace dfn::dsp
