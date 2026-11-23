#include "window.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <vector>

#include "support/reference.h"

namespace dfn::dsp {
namespace {

TEST(VorbisWindow, HasFftSizeCoefficients) {
    for (int fft_size : {480, 960, 1024, 2048}) {
        EXPECT_EQ(make_vorbis_window(fft_size).size(), static_cast<std::size_t>(fft_size));
    }
}

TEST(VorbisWindow, EndpointsAreNearlyZeroAndPeakIsAtTheCentre) {
    const std::vector<float> window = make_vorbis_window(960);
    EXPECT_LT(window.front(), 1e-4f);
    EXPECT_FLOAT_EQ(*std::max_element(window.begin(), window.end()), 1.0f);
    // The Vorbis window is flat around its centre: several samples round to exactly 1.0f in
    // float32, so the peak is asserted as a region rather than a single index.
    for (int i = 470; i <= 490; ++i) {
        EXPECT_GT(window[static_cast<std::size_t>(i)], 0.9999f) << "index " << i;
    }
}

TEST(VorbisWindow, IsSymmetric) {
    const std::vector<float> window = make_vorbis_window(960);
    for (int i = 0; i < 960; ++i) {
        ASSERT_FLOAT_EQ(window[static_cast<std::size_t>(i)],
                        window[static_cast<std::size_t>(959 - i)])
            << "index " << i;
    }
}

TEST(VorbisWindow, SatisfiesPrincenBradleyConditionForFiftyPercentOverlap) {
    // w[i]^2 + w[i + N/2]^2 == 1 is what makes the windowed analysis/synthesis pair
    // reconstruct the signal for hop = N/2.
    const std::vector<float> window = make_vorbis_window(960);
    for (int i = 0; i < 480; ++i) {
        const float sum =
            window[static_cast<std::size_t>(i)] * window[static_cast<std::size_t>(i)] +
            window[static_cast<std::size_t>(i + 480)] * window[static_cast<std::size_t>(i + 480)];
        ASSERT_NEAR(sum, 1.0f, 1e-5f) << "index " << i;
    }
}

TEST(VorbisWindow, MatchesUpstreamReferenceCoefficients) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<float> expected = test::load_f32("window_960");
    const std::vector<float> actual = make_vorbis_window(960);
    ASSERT_EQ(expected.size(), actual.size());
    const test::Metrics metrics =
        test::compute_metrics(expected.data(), actual.data(), expected.size());
    test::expect_max_abs("vorbis window (960)", metrics, 1e-7f);
}

TEST(AnalysisWindowNorm, MatchesUpstreamFormula) {
    // wnorm = 1 / (N^2 / (2 * hop)) = 2 * hop / N^2.
    EXPECT_FLOAT_EQ(analysis_window_norm(960, 480), 1.0f / 960.0f);
    EXPECT_FLOAT_EQ(analysis_window_norm(960, 240), 0.5f / 960.0f);
    EXPECT_FLOAT_EQ(make_analysis_window(960, 480).norm, 1.0f / 960.0f);
}

}  // namespace
}  // namespace dfn::dsp
