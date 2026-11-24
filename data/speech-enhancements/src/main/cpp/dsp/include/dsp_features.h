#ifndef DFN_DSP_FEATURES_H_
#define DFN_DSP_FEATURES_H_

// Feature extraction for the DeepFilterNet 3 encoder.
//
// Reference: libDF/src/lib.rs, `feat_erb`, `feat_cplx`, `feat_cplx_t`,
// `band_mean_norm_erb`, `band_unit_norm`, `band_unit_norm_t`, plus
// `init_mean_norm_state` / `init_unit_norm_state`.
//
// ERB features: band energies (mean power per band) -> dB -> exponential mean normalization
// (subtract a running mean, divide by 40).
// Complex features: exponential unit normalization of the first `nb_df` bins.

#include <complex>
#include <cstddef>
#include <span>
#include <vector>

namespace dfn::dsp {

// State initialization ranges from the reference implementation.
inline constexpr float kMeanNormInitMin = -60.0f;   // first ERB band (dB)
inline constexpr float kMeanNormInitMax = -90.0f;   // last ERB band (dB)
inline constexpr float kUnitNormInitMin = 0.001f;   // first DF bin
inline constexpr float kUnitNormInitMax = 0.0001f;  // last DF bin
inline constexpr float kErbFeatureScale = 1.0f / 40.0f;
inline constexpr float kErbEnergyEpsilon = 1e-10f;

// Persistent exponential normalization state for one audio channel.
struct FeatureNormState {
    std::vector<float> erb_state;   // size: number of ERB bands
    std::vector<float> unit_state;  // size: nb_df

    void reset(int nb_erb, int nb_df);
};

// Fills `state` with a linear interpolation from `min` to `max` (inclusive).
void init_linear_state(std::span<float> state, float min, float max);

// Computes `nb_erb` normalized ERB features for one spectrum.
// `state` and `out` must have `widths.size()` entries.
void feat_erb(std::span<const std::complex<float>> spectrum, std::span<const int> widths,
              float alpha, std::span<float> state, std::span<float> out);

// Computes `nb_df` normalized complex features (interleaved complex layout).
void feat_cplx(std::span<const std::complex<float>> spectrum, std::size_t nb_df, float alpha,
               std::span<float> state, std::span<std::complex<float>> out);

// Same as feat_cplx, but writes the layout used by the ONNX encoder input:
// out[0 .. nb_df) holds the real parts, out[nb_df .. 2*nb_df) the imaginary parts.
void feat_cplx_transposed(std::span<const std::complex<float>> spectrum, std::size_t nb_df,
                          float alpha, std::span<float> state, std::span<float> out);

// Runs feat_erb over every frame of a spectrogram. `spectrogram` holds
// `frame_count * widths_sum` interleaved complex values.
void feat_erb_sequence(std::span<const std::complex<float>> spectrogram, std::size_t frame_count,
                       std::span<const int> widths, float alpha, std::span<float> state,
                       std::span<float> out);

}  // namespace dfn::dsp

#endif  // DFN_DSP_FEATURES_H_
