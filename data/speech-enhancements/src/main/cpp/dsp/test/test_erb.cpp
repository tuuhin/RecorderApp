#include "erb.h"

#include <gtest/gtest.h>

#include <cmath>
#include <complex>
#include <vector>

#include "support/reference.h"

namespace dfn::dsp {
namespace {

constexpr int kSampleRate = 48000;
constexpr int kFftSize = 960;
constexpr int kFreqBins = kFftSize / 2 + 1;
constexpr int kErbBands = 32;

TEST(ErbScale, RoundTripsBetweenHzAndErb) {
    for (float hz : {0.0f, 100.0f, 440.0f, 1000.0f, 4000.0f, 12000.0f, 24000.0f}) {
        EXPECT_NEAR(hz_from_erb(erb_from_hz(hz)), hz, std::max(1e-2f, hz * 1e-5f)) << "hz=" << hz;
    }
    EXPECT_FLOAT_EQ(erb_from_hz(0.0f), 0.0f);
}

TEST(ErbFilterbank, DimensionsAndCoverage) {
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    ASSERT_EQ(widths.size(), static_cast<std::size_t>(kErbBands));
    EXPECT_EQ(erb_total_width(widths), kFreqBins);
    for (const int width : widths) {
        EXPECT_GE(width, 2);
    }
    // Band widths grow monotonically with frequency (ERB scale).
    for (std::size_t i = 1; i < widths.size(); ++i) {
        EXPECT_GE(widths[i], widths[i - 1]) << "band " << i;
    }
}

TEST(ErbFilterbank, MatchesUpstreamBandWidths) {
    if (!test::reference_available()) {
        GTEST_SKIP() << "reference data missing; run test/support/generate_reference.py";
    }
    const std::vector<float> expected_float = test::load_f32("erb_widths");
    const std::vector<int> actual = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    ASSERT_EQ(expected_float.size(), actual.size());
    for (std::size_t i = 0; i < actual.size(); ++i) {
        EXPECT_EQ(actual[i], static_cast<int>(expected_float[i])) << "band " << i;
    }
    // Values also documented in the upstream repository for DeepFilterNet 3.
    const std::vector<int> upstream = {2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
                                       2,  2,  5,  5,  7,  7,  8,  10, 12, 13, 15,
                                       18, 20, 24, 28, 31, 37, 42, 50, 56, 67};
    EXPECT_EQ(actual, upstream);
}

TEST(ErbFilterbank, RejectsInvalidArguments) {
    EXPECT_THROW(erb_band_widths(0, kFftSize, kErbBands, 2), std::invalid_argument);
    EXPECT_THROW(erb_band_widths(kSampleRate, 0, kErbBands, 2), std::invalid_argument);
    EXPECT_THROW(erb_band_widths(kSampleRate, kFftSize, 0, 2), std::invalid_argument);
}

TEST(ErbFilterbank, ZeroInputProducesZeroBands) {
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {0.0f, 0.0f});
    std::vector<float> bands(static_cast<std::size_t>(kErbBands), -1.0f);
    erb_band_correlation(spectrum, spectrum, widths, bands);
    for (const float value : bands) {
        EXPECT_FLOAT_EQ(value, 0.0f);
    }
}

TEST(ErbFilterbank, KnownFrequencyBinInput) {
    // A spectrum that is one in every bin yields band correlations of exactly one, because
    // each band averages |x|^2 = 1 over its bins.
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {1.0f, 0.0f});
    std::vector<float> bands(static_cast<std::size_t>(kErbBands), 0.0f);
    erb_band_correlation(spectrum, spectrum, widths, bands);
    for (std::size_t b = 0; b < bands.size(); ++b) {
        EXPECT_NEAR(bands[b], 1.0f, 1e-6f) << "band " << b;
    }
}

TEST(ErbFilterbank, SingleBinInputOnlyAffectsItsBand) {
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {0.0f, 0.0f});
    // Bin 100 lies in a higher band than bin 0; check exact band arithmetic from the widths.
    int band_index = 0;
    int offset = 0;
    while (offset + widths[static_cast<std::size_t>(band_index)] <= 100) {
        offset += widths[static_cast<std::size_t>(band_index)];
        ++band_index;
    }
    spectrum[100] = std::complex<float>(2.0f, 0.0f);
    std::vector<float> bands(static_cast<std::size_t>(kErbBands), 0.0f);
    erb_band_correlation(spectrum, spectrum, widths, bands);
    for (int b = 0; b < kErbBands; ++b) {
        if (b == band_index) {
            EXPECT_NEAR(bands[static_cast<std::size_t>(b)],
                        4.0f / static_cast<float>(widths[static_cast<std::size_t>(b)]),
                        1e-6f);
        } else {
            EXPECT_FLOAT_EQ(bands[static_cast<std::size_t>(b)], 0.0f) << "band " << b;
        }
    }
}

TEST(ErbFilterbank, BandGainApplicationAndInterpolation) {
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    std::vector<std::complex<float>> spectrum(static_cast<std::size_t>(kFreqBins), {1.0f, 2.0f});
    std::vector<float> gains(static_cast<std::size_t>(kErbBands), 1.0f);
    gains[3] = 0.25f;
    gains[static_cast<std::size_t>(kErbBands - 1)] = 0.5f;
    erb_apply_interp_band_gain(spectrum, widths, gains);

    int offset = 0;
    for (int b = 0; b < kErbBands; ++b) {
        for (int j = 0; j < widths[static_cast<std::size_t>(b)]; ++j) {
            EXPECT_FLOAT_EQ(spectrum[static_cast<std::size_t>(offset + j)].real(),
                            gains[static_cast<std::size_t>(b)]);
            EXPECT_FLOAT_EQ(spectrum[static_cast<std::size_t>(offset + j)].imag(),
                            2.0f * gains[static_cast<std::size_t>(b)]);
        }
        offset += widths[static_cast<std::size_t>(b)];
    }

    std::vector<float> interpolated(static_cast<std::size_t>(kFreqBins), 0.0f);
    erb_interp_band_gain(interpolated, widths, gains);
    int band = 0;
    int bin = 0;
    for (int b = 0; b < kErbBands; ++b) {
        for (int j = 0; j < widths[static_cast<std::size_t>(b)]; ++j, ++bin) {
            EXPECT_FLOAT_EQ(interpolated[static_cast<std::size_t>(bin)],
                            gains[static_cast<std::size_t>(b)]);
        }
        ++band;
    }
    EXPECT_EQ(bin, kFreqBins);
    EXPECT_EQ(band, kErbBands);
}

TEST(ErbFilterbank, CompressionIsTheBandMean) {
    const std::vector<int> widths = erb_band_widths(kSampleRate, kFftSize, kErbBands, 2);
    std::vector<float> values(static_cast<std::size_t>(kFreqBins));
    for (int k = 0; k < kFreqBins; ++k) {
        values[static_cast<std::size_t>(k)] = static_cast<float>(k);
    }
    std::vector<float> bands(static_cast<std::size_t>(kErbBands), 0.0f);
    erb_band_compress(values, widths, bands);
    int offset = 0;
    for (int b = 0; b < kErbBands; ++b) {
        const int width = widths[static_cast<std::size_t>(b)];
        float expected = 0.0f;
        for (int j = 0; j < width; ++j) {
            expected += static_cast<float>(offset + j);
        }
        expected /= static_cast<float>(width);
        EXPECT_NEAR(bands[static_cast<std::size_t>(b)], expected, 1e-3f) << "band " << b;
        offset += width;
    }
}

}  // namespace
}  // namespace dfn::dsp
