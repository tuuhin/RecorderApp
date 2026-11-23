#include "deep_filter.h"

#include <gtest/gtest.h>

#include <cmath>
#include <complex>
#include <vector>

#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr int kNbDf = 96;
constexpr int kFreqBins = 481;
constexpr int kDfOrder = 5;

std::vector<std::complex<float>> make_history() {
    std::vector<std::complex<float>> history(static_cast<std::size_t>(kDfOrder * kFreqBins));
    for (int k = 0; k < kDfOrder; ++k) {
        for (int f = 0; f < kFreqBins; ++f) {
            const auto index = static_cast<std::size_t>(k * kFreqBins + f);
            history[index] =
                std::complex<float>(0.01f * static_cast<float>(k + 1) * static_cast<float>(f + 1),
                                    -0.005f * static_cast<float>(k + 1));
        }
    }
    return history;
}

TEST(DeepFilter, IdentityCoefficientsSelectTheFirstHistoryFrame) {
    const std::vector<std::complex<float>> history = make_history();
    std::vector<std::complex<float>> coefs(static_cast<std::size_t>(kDfOrder * kNbDf),
                                           {0.0f, 0.0f});
    for (int f = 0; f < kNbDf; ++f) {
        coefs[static_cast<std::size_t>(f)] = std::complex<float>(1.0f, 0.0f);  // k = 0
    }
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {9.0f, 9.0f});
    apply_deep_filter(history.data(), kDfOrder, coefs.data(), kNbDf, kFreqBins, spectrum.data());

    for (int f = 0; f < kNbDf; ++f) {
        ASSERT_FLOAT_EQ(spectrum[static_cast<std::size_t>(f)].real(),
                        history[static_cast<std::size_t>(f)].real())
            << "bin " << f;
        ASSERT_FLOAT_EQ(spectrum[static_cast<std::size_t>(f)].imag(),
                        history[static_cast<std::size_t>(f)].imag())
            << "bin " << f;
    }
    // Bins above nb_df keep the stage-1 spectrum untouched.
    for (int f = kNbDf; f < kFreqBins; ++f) {
        EXPECT_EQ(spectrum[static_cast<std::size_t>(f)], std::complex<float>(9.0f, 9.0f))
            << "bin " << f;
    }
}

TEST(DeepFilter, ZeroCoefficientsZeroTheFilteredBins) {
    const std::vector<std::complex<float>> history = make_history();
    std::vector<std::complex<float>> coefs(static_cast<std::size_t>(kDfOrder * kNbDf),
                                           {0.0f, 0.0f});
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {9.0f, 9.0f});
    apply_deep_filter(history.data(), kDfOrder, coefs.data(), kNbDf, kFreqBins, spectrum.data());
    for (int f = 0; f < kNbDf; ++f) {
        EXPECT_EQ(spectrum[static_cast<std::size_t>(f)], std::complex<float>(0.0f, 0.0f))
            << "bin " << f;
    }
    for (int f = kNbDf; f < kFreqBins; ++f) {
        EXPECT_EQ(spectrum[static_cast<std::size_t>(f)], std::complex<float>(9.0f, 9.0f))
            << "bin " << f;
    }
}

TEST(DeepFilter, KnownComplexCoefficientCalculation) {
    // Single bin, two taps, hand-computed complex arithmetic.
    const std::complex<float> s0(1.0f, 2.0f);
    const std::complex<float> s1(-3.0f, 0.5f);
    const std::complex<float> c0(0.5f, -1.0f);
    const std::complex<float> c1(2.0f, 0.25f);
    const std::vector<std::complex<float>> history = {s0, s1};
    const std::vector<std::complex<float>> coefs = {c0, c1};
    std::vector<std::complex<float>> spectrum = {std::complex<float>(7.0f, 7.0f)};
    apply_deep_filter(history.data(), 2, coefs.data(), 1, 1, spectrum.data());

    const std::complex<float> expected = s0 * c0 + s1 * c1;
    EXPECT_NEAR(spectrum[0].real(), expected.real(), 1e-6f);
    EXPECT_NEAR(spectrum[0].imag(), expected.imag(), 1e-6f);
    // s0*c0 = (1+2i)(0.5-1i) = 0.5 -1i +1i +2 = 2.5 + 0i
    // s1*c1 = (-3+0.5i)(2+0.25i) = -6 -0.75i +1i -0.125 = -6.125 + 0.25i
    EXPECT_NEAR(spectrum[0].real(), -3.625f, 1e-6f);
    EXPECT_NEAR(spectrum[0].imag(), 0.25f, 1e-6f);
}

TEST(DeepFilter, SpectralHistoryIsPreservedAndOrdered) {
    // Coefficients selecting the last tap must pick the newest frame.
    const std::vector<std::complex<float>> history = make_history();
    std::vector<std::complex<float>> coefs(static_cast<std::size_t>(kDfOrder * kNbDf),
                                           {0.0f, 0.0f});
    for (int f = 0; f < kNbDf; ++f) {
        coefs[static_cast<std::size_t>((kDfOrder - 1) * kNbDf + f)] =
            std::complex<float>(1.0f, 0.0f);
    }
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {0.0f, 0.0f});
    apply_deep_filter(history.data(), kDfOrder, coefs.data(), kNbDf, kFreqBins, spectrum.data());
    for (int f = 0; f < kNbDf; ++f) {
        const auto expected = history[static_cast<std::size_t>((kDfOrder - 1) * kFreqBins + f)];
        EXPECT_FLOAT_EQ(spectrum[static_cast<std::size_t>(f)].real(), expected.real());
        EXPECT_FLOAT_EQ(spectrum[static_cast<std::size_t>(f)].imag(), expected.imag());
    }
}

TEST(DeepFilter, InitialHistoryOfZerosYieldsZeros) {
    std::vector<std::complex<float>> history(static_cast<std::size_t>(kDfOrder * kFreqBins),
                                             {0.0f, 0.0f});
    std::vector<std::complex<float>> coefs(static_cast<std::size_t>(kDfOrder * kNbDf),
                                           {1.0f, 0.0f});
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {3.0f, 3.0f});
    apply_deep_filter(history.data(), kDfOrder, coefs.data(), kNbDf, kFreqBins, spectrum.data());
    for (int f = 0; f < kNbDf; ++f) {
        EXPECT_EQ(spectrum[static_cast<std::size_t>(f)], std::complex<float>(0.0f, 0.0f));
    }
}

TEST(PostFilter, BetaZeroIsIdentity) {
    std::vector<std::complex<float>> noisy = {{0.5f, 0.1f}, {0.2f, -0.3f}};
    std::vector<std::complex<float>> enhanced = {{0.4f, 0.05f}, {0.1f, -0.2f}};
    const std::vector<std::complex<float>> before = enhanced;
    apply_post_filter(noisy.data(), enhanced.data(), 2, 0.0f);
    for (std::size_t i = 0; i < enhanced.size(); ++i) {
        EXPECT_FLOAT_EQ(enhanced[i].real(), before[i].real());
        EXPECT_FLOAT_EQ(enhanced[i].imag(), before[i].imag());
    }
}

TEST(PostFilter, ReducesMagnitudeMonotonicallyWithBeta) {
    std::vector<std::complex<float>> noisy = {{1.0f, 0.0f}};
    std::vector<std::complex<float>> enhanced = {{0.5f, 0.0f}};
    apply_post_filter(noisy.data(), enhanced.data(), 1, 0.02f);
    const float magnitude = std::abs(enhanced[0]);
    EXPECT_LT(magnitude, 0.5f);
    EXPECT_GT(magnitude, 0.4f);

    // Unity gain is a fixed point: when the enhanced and noisy magnitudes match, the
    // post-filter leaves the spectrum unchanged.
    enhanced[0] = std::complex<float>(1.0f, 0.0f);
    apply_post_filter(noisy.data(), enhanced.data(), 1, 0.02f);
    EXPECT_NEAR(std::abs(enhanced[0]), 1.0f, 1e-6f);
}

TEST(AttenuationLimit, BlendsNoisyAndEnhanced) {
    std::vector<std::complex<float>> noisy = {{1.0f, 0.0f}};
    std::vector<std::complex<float>> enhanced = {{0.0f, 0.0f}};
    apply_attenuation_limit(noisy.data(), enhanced.data(), 1, 1.0f);
    EXPECT_FLOAT_EQ(enhanced[0].real(), 1.0f);
    enhanced[0] = std::complex<float>(0.0f, 0.0f);
    apply_attenuation_limit(noisy.data(), enhanced.data(), 1, 0.0f);
    EXPECT_FLOAT_EQ(enhanced[0].real(), 0.0f);
    enhanced[0] = std::complex<float>(0.0f, 0.0f);
    apply_attenuation_limit(noisy.data(), enhanced.data(), 1, 0.25f);
    EXPECT_FLOAT_EQ(enhanced[0].real(), 0.25f);
}

}  // namespace
}  // namespace dfn::dsp
