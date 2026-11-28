#ifndef DFN_DSP_WINDOW_H_
#define DFN_DSP_WINDOW_H_

// Analysis/synthesis window used by DeepFilterNet.
//
// Reference: libDF/src/lib.rs (DFState::new) builds a Vorbis window
//     w[i] = sin(pi/2 * sin^2(pi * (i + 0.5) / N))
// and normalizes the analysis spectrum by wnorm = 2 * hop / N^2 so that a windowed
// analysis followed by a windowed synthesis reconstructs the signal with 50% overlap.

#include <vector>

namespace dfn::dsp {

// Returns the Vorbis window of length `fft_size`. Computed in double precision and stored
// as float32, exactly like the reference implementation.
std::vector<float> make_vorbis_window(int fft_size);

// Analysis normalization factor 2 * hop_size / fft_size^2.
float analysis_window_norm(int fft_size, int hop_size);

struct analysis_window {
    std::vector<float> coefficients;
    float norm = 1.0f;
};

analysis_window make_analysis_window(int fft_size, int hop_size);

}  // namespace dfn::dsp

#endif  // DFN_DSP_WINDOW_H_
