#ifndef DFN_ENHANCER_ENHANCER_STATE_H_
#define DFN_ENHANCER_ENHANCER_STATE_H_

// Persistent processing state of one enhancement stream.
//
// Every buffer here has a lifetime of the whole stream: the STFT overlap memories, the
// recurrent ONNX states, the feature normalization states and the rolling spectrogram /
// feature windows. `reset()` restores the exact state of a freshly created session.

#include <complex>
#include <cstddef>
#include <vector>

#include "dsp_features.h"
#include "istft.h"
#include "model_config.h"
#include "onnx_backend.h"
#include "stft.h"

namespace dfn::enhancer {

// Fixed-size ring of complex spectra that keeps its contents contiguous and ordered
// oldest -> newest.
class FrameRing {
   public:
    FrameRing() = default;
    FrameRing(int capacity_frames, int frame_size);

    void reset();
    void push(const std::complex<float>* frame);

    int capacity_frames() const { return capacity_frames_; }
    int frame_size() const { return frame_size_; }
    const std::complex<float>* data() const { return data_.data(); }

    // Frame at `index`, counted from the oldest frame.
    const std::complex<float>* frame(int index) const {
        return data_.data() +
               static_cast<std::size_t>(index) * static_cast<std::size_t>(frame_size_);
    }

   private:
    int capacity_frames_ = 0;
    int frame_size_ = 0;
    std::vector<std::complex<float>> data_;
};

struct EnhancerState {
    explicit EnhancerState(ModelConfig config);

    void reset();

    ModelConfig config;
    dsp::Stft stft;
    dsp::Istft istft;
    dsp::FeatureNormState normalization;
    RecurrentState recurrent;

    // Rolling model input windows (feature history for the causal convolutions).
    std::vector<float> erb_window;   // [encoder_window][nb_erb]
    std::vector<float> spec_window;  // [2][encoder_window][nb_df]
    std::vector<float> emb_window;   // [df_window][erb_hidden]
    std::vector<float> c0_window;    // [df_window][conv_ch][nb_df]

    // Rolling spectra used by the mask / deep-filter stages.
    FrameRing rolling_y;  // df_order + conv_lookahead frames
    FrameRing rolling_x;  // max(df_order, lookahead) frames

    // Scratch buffers for one frame.
    std::vector<std::complex<float>> spectrum;    // current noisy spectrum
    std::vector<std::complex<float>> enhanced;    // spectrum handed to the ISTFT
    std::vector<std::complex<float>> df_history;  // df_order contiguous frames for DF
    std::vector<float> erb_feature;               // nb_erb
    std::vector<float> df_coefs;                  // df_order*nb_df*2
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_ENHANCER_STATE_H_
