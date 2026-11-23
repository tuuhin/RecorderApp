#include "dsp_features.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <complex>
#include <vector>

#include "erb.h"
#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr int kSampleRate = 48000;
constexpr int kFftSize = 960;
constexpr int kFreqBins = kFftSize / 2 + 1;
constexpr int kErbBands = 32;
constexpr int kNbDf = 96;
constexpr float kAlpha = 0.99f;

std::vector<int> widths() {
    return erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
}

TEST(FeatureNormalizationState, InitialStateMatchesUpstreamLinearRamp) {
    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    ASSERT_EQ(state.erb_state.size(), static_cast<std::size_t>(kErbBands));
    ASSERT_EQ(state.unit_state.size(), static_cast<std::size_t>(kNbDf));
    EXPECT_NEAR(state.erb_state.front(), -60.0f, 1e-5f);
    EXPECT_NEAR(state.erb_state.back(), -90.0f, 1e-3f);
    EXPECT_NEAR(state.unit_state.front(), 0.001f, 1e-7f);
    EXPECT_NEAR(state.unit_state.back(), 0.0001f, 1e-7f);
    for (std::size_t i = 1; i < state.erb_state.size(); ++i) {
        EXPECT_LT(state.erb_state[i], state.erb_state[i - 1]);
    }
    for (std::size_t i = 1; i < state.unit_state.size(); ++i) {
        EXPECT_LT(state.unit_state[i], state.unit_state[i - 1]);
    }
}

TEST(ErbFeatures, TensorDimensions) {
    const std::vector<int> bands = widths();
    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {1.0f, 0.0f});
    std::vector<float> features(static_cast<std::size_t>(kErbBands));
    feat_erb(spectrum.data(), bands, kAlpha, state.erb_state.data(), features.data());
    ASSERT_EQ(features.size(), static_cast<std::size_t>(kErbBands));
}

TEST(ErbFeatures, SilenceStaysFiniteAndNormalized) {
    const std::vector<int> bands = widths();
    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {0.0f, 0.0f});
    std::vector<float> features(static_cast<std::size_t>(kErbBands));
    feat_erb(spectrum.data(), bands, kAlpha, state.erb_state.data(), features.data());
    for (const float value : features) {
        EXPECT_TRUE(std::isfinite(value));
    }
    // With an initial state around -60..-90 dB, the 10*log10(1e-10) = -100 dB floor keeps
    // silence features negative and bounded.
    const auto [min_it, max_it] = std::minmax_element(features.begin(), features.end());
    EXPECT_LT(*max_it, 2.0f);
    EXPECT_GT(*min_it, -2.0f);
}

TEST(ErbFeatures, MatchesUpstreamReference) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<std::complex<float>> spectrogram = test::load_complex("erb_feat_input");
    const std::vector<float> expected = test::load_f32("erb_feat");
    const std::vector<int> shape = test::load_shape("erb_feat");
    ASSERT_EQ(shape.size(), 2u);
    const int frames = shape[0];
    ASSERT_EQ(shape[1], kErbBands);

    const std::vector<int> bands = widths();
    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    std::vector<float> actual(static_cast<std::size_t>(frames * kErbBands));
    feat_erb_sequence(
        spectrogram.data(), frames, bands, kAlpha, state.erb_state.data(), actual.data());

    const test::Metrics metrics =
        test::compute_metrics(expected.data(), actual.data(), expected.size());
    test::expect_max_abs("erb features [4,32] float32", metrics, 1e-4f);
}

TEST(ComplexFeatures, MatchesUpstreamReference) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<std::complex<float>> spectrogram = test::load_complex("erb_feat_input");
    const std::vector<std::complex<float>> expected = test::load_complex("unit_norm");
    const std::vector<int> shape = test::load_shape("unit_norm");
    ASSERT_EQ(shape.size(), 2u);
    const int frames = shape[0];
    ASSERT_EQ(shape[1], kNbDf);

    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    std::vector<std::complex<float>> actual(static_cast<std::size_t>(frames * kNbDf));
    for (int f = 0; f < frames; ++f) {
        feat_cplx(spectrogram.data() + static_cast<std::size_t>(f) * kFreqBins,
                  kNbDf,
                  kAlpha,
                  state.unit_state.data(),
                  actual.data() + static_cast<std::size_t>(f) * kNbDf);
    }

    const test::Metrics metrics =
        test::compute_metrics(expected.data(), actual.data(), expected.size());
    test::expect_max_abs("unit-normalized complex features [4,96] complex64", metrics, 1e-4f);
}

TEST(ComplexFeatures, TransposedLayoutMatchesInterleavedLayout) {
    const std::vector<int> bands = widths();
    FeatureNormState state_a;
    FeatureNormState state_b;
    state_a.reset(kErbBands, kNbDf);
    state_b.reset(kErbBands, kNbDf);

    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins));
    for (int k = 0; k < kFreqBins; ++k) {
        spectrum[static_cast<std::size_t>(k)] = std::complex<float>(
            std::sin(0.37f * static_cast<float>(k)), std::cos(0.11f * static_cast<float>(k)));
    }
    std::vector<std::complex<float>> interleaved(static_cast<std::size_t>(kNbDf));
    feat_cplx(spectrum.data(), kNbDf, kAlpha, state_a.unit_state.data(), interleaved.data());

    std::vector<float> transposed(static_cast<std::size_t>(2 * kNbDf));
    feat_cplx_transposed(
        spectrum.data(), kNbDf, kAlpha, state_b.unit_state.data(), transposed.data());
    for (int k = 0; k < kNbDf; ++k) {
        EXPECT_FLOAT_EQ(transposed[static_cast<std::size_t>(k)],
                        interleaved[static_cast<std::size_t>(k)].real());
        EXPECT_FLOAT_EQ(transposed[static_cast<std::size_t>(kNbDf + k)],
                        interleaved[static_cast<std::size_t>(k)].imag());
    }
    // The transposed variant must not disturb the normalization state.
    for (int k = 0; k < kNbDf; ++k) {
        EXPECT_FLOAT_EQ(state_a.unit_state[static_cast<std::size_t>(k)],
                        state_b.unit_state[static_cast<std::size_t>(k)]);
    }
}

TEST(ComplexFeatures, NormalizationHasUnitScale) {
    // After many frames of a stationary tone the running magnitude converges, so the
    // normalized spectrum has order-one magnitude (this is what the network expects).
    FeatureNormState state;
    state.reset(kErbBands, kNbDf);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {0.0f, 0.0f});
    for (int k = 0; k < kNbDf; ++k) {
        spectrum[static_cast<std::size_t>(k)] = std::complex<float>(0.05f, -0.02f);
    }
    std::vector<std::complex<float>> out(static_cast<std::size_t>(kNbDf));
    for (int iteration = 0; iteration < 2000; ++iteration) {
        feat_cplx(spectrum.data(), kNbDf, kAlpha, state.unit_state.data(), out.data());
    }
    // For a stationary input the state converges to |x| = sqrt(0.05^2 + 0.02^2), so the
    // normalized magnitude converges to |x| / sqrt(|x|) = sqrt(|x|) ~= 0.232.
    const float magnitude = std::abs(out.front());
    const float expected = std::sqrt(std::sqrt(0.05f * 0.05f + 0.02f * 0.02f));
    EXPECT_NEAR(magnitude, expected, 2e-3f);
}

}  // namespace
}  // namespace dfn::dsp
