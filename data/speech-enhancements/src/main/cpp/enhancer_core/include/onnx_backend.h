#ifndef DFN_ENHANCER_ONNX_BACKEND_H_
#define DFN_ENHANCER_ONNX_BACKEND_H_

// ONNX Runtime integration for the three DeepFilterNet 3 graphs (encoder, ERB decoder,
// deep-filter decoder).
//
// The graphs are expected to be the streaming variants produced by
// `tools/prepare_models.py`: one output frame per call, explicit recurrent state inputs
// (`state_in_<i>` / `state_out_<i>`) and causal-convolution context supplied by the caller
// through a rolling input window. Both the graph layout and the tensor shapes are inspected
// at load time and validated against the model configuration; nothing is assumed.

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "model_config.h"

namespace dfn::enhancer {

// Output of one encoder step (a single frame).
struct EncoderResult {
    std::vector<float> e0;   // [conv_ch][nb_erb]
    std::vector<float> e1;   // [conv_ch][nb_erb/2]
    std::vector<float> e2;   // [conv_ch][nb_erb/4]
    std::vector<float> e3;   // [conv_ch][nb_erb/4]
    std::vector<float> emb;  // [erb_hidden]
    std::vector<float> c0;   // [conv_ch][nb_df]
    float lsnr = 0.0f;
};

struct TensorSpec {
    std::string name;
    std::vector<int64_t> shape;  // -1 for dynamic dimensions
    int32_t element_type = 0;    // ONNX TensorProto element type
};

// Persistent recurrent state of the three graphs.
struct RecurrentState {
    std::vector<std::vector<float>> encoder;
    std::vector<std::vector<float>> erb_decoder;
    std::vector<std::vector<float>> df_decoder;

    void clear() {
        encoder.clear();
        erb_decoder.clear();
        df_decoder.clear();
    }
};

// Abstraction over the three networks so the orchestration can be unit tested without
// ONNX Runtime.
class ModelRunner {
   public:
    virtual ~ModelRunner() = default;

    // Encoder: `feat_erb` is [window][nb_erb], `feat_spec` is [2][window][nb_df].
    virtual EncoderResult run_encoder(const float* feat_erb, int window_frames,
                                      const float* feat_spec, RecurrentState& state) = 0;

    // ERB mask decoder for the current frame; returns nb_erb values.
    virtual const std::vector<float>& run_erb_decoder(const EncoderResult& encoder,
                                                      RecurrentState& state) = 0;

    // Deep-filter coefficients for the current frame; returns df_order*nb_df*2 values
    // laid out as [df_order][nb_df][2].
    virtual const std::vector<float>& run_df_decoder(const float* emb_window,
                                                     const float* c0_window,
                                                     RecurrentState& state) = 0;

    // Creates the zero-initialized recurrent state for every graph.
    virtual void init_state(RecurrentState& state) const = 0;
};

class OnnxBackend : public ModelRunner {
   public:
    struct Options {
        int intra_op_threads = 1;
        int inter_op_threads = 1;
        bool use_nnapi = false;
        int graph_optimization_level = 99;  // ORT_ENABLE_ALL
    };

    static std::unique_ptr<OnnxBackend> from_files(const std::string& encoder_path,
                                                   const std::string& erb_decoder_path,
                                                   const std::string& df_decoder_path,
                                                   const ModelConfig& config, Options options,
                                                   std::string* error);
    static std::unique_ptr<OnnxBackend> from_memory(const std::vector<uint8_t>& encoder,
                                                    const std::vector<uint8_t>& erb_decoder,
                                                    const std::vector<uint8_t>& df_decoder,
                                                    const ModelConfig& config, Options options,
                                                    std::string* error);

    ~OnnxBackend() override;

    [[nodiscard]] const std::vector<TensorSpec>& encoder_inputs() const;
    [[nodiscard]] const std::vector<TensorSpec>& encoder_outputs() const;
    [[nodiscard]] const std::vector<TensorSpec>& erb_decoder_inputs() const;
    [[nodiscard]] const std::vector<TensorSpec>& df_decoder_inputs() const;

    EncoderResult run_encoder(const float* feat_erb, int window_frames, const float* feat_spec,
                              RecurrentState& state) override;
    const std::vector<float>& run_erb_decoder(const EncoderResult& encoder,
                                              RecurrentState& state) override;
    const std::vector<float>& run_df_decoder(const float* emb_window, const float* c0_window,
                                             RecurrentState& state) override;
    void init_state(RecurrentState& state) const override;

   private:
    struct Impl;
    OnnxBackend();
    bool _initialize(const ModelConfig& config, std::string* error);

    std::unique_ptr<Impl> impl_;
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_ONNX_BACKEND_H_
