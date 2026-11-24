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
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include "model_config.h"

namespace dfn::enhancer {

// Output of one encoder step (a single frame).
struct encoder_result {
    std::vector<float> e0;   // [conv_ch][nb_erb]
    std::vector<float> e1;   // [conv_ch][nb_erb/2]
    std::vector<float> e2;   // [conv_ch][nb_erb/4]
    std::vector<float> e3;   // [conv_ch][nb_erb/4]
    std::vector<float> emb;  // [erb_hidden]
    std::vector<float> c0;   // [conv_ch][nb_df]
    float lsnr = 0.0f;
};

struct tensor {
    std::string name;
    std::vector<int64_t> shape;  // -1 for dynamic dimensions
    int32_t element_type = 0;    // ONNX TensorProto element type
};

// Persistent recurrent state of the three graphs.
struct recurrent_state {
    std::vector<std::vector<float>> encoder;
    std::vector<std::vector<float>> erb_decoder;
    std::vector<std::vector<float>> df_decoder;

    void clear() noexcept {
        encoder.clear();
        erb_decoder.clear();
        df_decoder.clear();
    }
};

// Abstraction over the three networks so the orchestration can be unit tested without
// ONNX Runtime.
class model_runner {
   public:
    virtual ~model_runner() = default;

    // Encoder: `feat_erb` is [window][nb_erb], `feat_spec` is [2][window][nb_df].
    virtual encoder_result run_encoder(std::span<const float> feat_erb, int window_frames,
                                       std::span<const float> feat_spec,
                                       recurrent_state& state) = 0;

    // ERB mask decoder for the current frame; returns nb_erb values.
    virtual const std::vector<float>& run_erb_decoder(const encoder_result& encoder,
                                                      recurrent_state& state) = 0;

    // Deep-filter coefficients for the current frame; returns df_order*nb_df*2 values
    // laid out as [df_order][nb_df][2].
    virtual const std::vector<float>& run_df_decoder(std::span<const float> emb_window,
                                                     std::span<const float> c0_window,
                                                     recurrent_state& state) = 0;

    // Creates the zero-initialized recurrent state for every graph.
    virtual void init_state(recurrent_state& state) const = 0;
};

class onnx_backend_runner : public model_runner {
   public:
    struct Options {
        int intra_op_threads = 1;
        int inter_op_threads = 1;
        bool use_nnapi = false;
        int graph_optimization_level = 99;  // ORT_ENABLE_ALL
    };

    [[nodiscard]] static std::unique_ptr<onnx_backend_runner> from_files(
        std::string_view encoder_path, std::string_view erb_decoder_path,
        std::string_view df_decoder_path, const model_config& config, Options options,
        std::string* error);

    [[nodiscard]] static std::unique_ptr<onnx_backend_runner> from_memory(
        std::span<const std::uint8_t> encoder, std::span<const std::uint8_t> erb_decoder,
        std::span<const std::uint8_t> df_decoder, const model_config& config, Options options,
        std::string* error);

    ~onnx_backend_runner() override;

    [[nodiscard]] const std::vector<tensor>& encoder_inputs() const noexcept;
    [[nodiscard]] const std::vector<tensor>& encoder_outputs() const noexcept;
    [[nodiscard]] const std::vector<tensor>& erb_decoder_inputs() const noexcept;
    [[nodiscard]] const std::vector<tensor>& df_decoder_inputs() const noexcept;

    encoder_result run_encoder(std::span<const float> feat_erb, int window_frames,
                               std::span<const float> feat_spec, recurrent_state& state) override;
    const std::vector<float>& run_erb_decoder(const encoder_result& encoder,
                                              recurrent_state& state) override;
    const std::vector<float>& run_df_decoder(std::span<const float> emb_window,
                                             std::span<const float> c0_window,
                                             recurrent_state& state) override;
    void init_state(recurrent_state& state) const override;

   private:
    struct Impl;
    onnx_backend_runner();
    bool init(const model_config& config, std::string* error);

    std::unique_ptr<Impl> impl_;
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_ONNX_BACKEND_H_
