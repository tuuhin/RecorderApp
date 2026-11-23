#ifndef DFN_TEST_SUPPORT_STUB_RUNNER_H_
#define DFN_TEST_SUPPORT_STUB_RUNNER_H_

// Deterministic ModelRunner used to test the orchestration and streaming behaviour without
// ONNX Runtime or model files. Its outputs depend on the inputs and on the recurrent state,
// so state persistence, chunking invariance and reset semantics are all observable.

#include <cmath>
#include <vector>

#include "onnx_backend.h"

namespace dfn::test {

class StubRunner : public enhancer::ModelRunner {
   public:
    explicit StubRunner(const enhancer::ModelConfig& config) : config_(config) {}

    void init_state(enhancer::RecurrentState& state) const override {
        state.encoder.assign(1, std::vector<float>(256, 0.0f));
        state.erb_decoder.assign(2, std::vector<float>(256, 0.0f));
        state.df_decoder.assign(2, std::vector<float>(256, 0.0f));
    }

    enhancer::EncoderResult run_encoder(const float* feat_erb, int window_frames,
                                        const float* feat_spec,
                                        enhancer::RecurrentState& state) override {
        (void)feat_spec;
        if (state.encoder.empty()) {
            init_state(state);
        }
        const auto nb_erb = static_cast<std::size_t>(config_.nb_erb);
        float sum = 0.0f;
        for (std::size_t i = 0; i < nb_erb; ++i) {
            sum += feat_erb[(static_cast<std::size_t>(window_frames) - 1) * nb_erb + i];
        }
        const float mean = sum / static_cast<float>(nb_erb);

        // Advance a frame counter kept in the recurrent state.
        float& counter = state.encoder[0][0];
        counter += 1.0f;

        enhancer::EncoderResult result;
        result.lsnr = mean * 10.0f;
        result.e0.assign(static_cast<std::size_t>(config_.conv_ch * config_.nb_erb), 0.0f);
        result.e1.assign(static_cast<std::size_t>(config_.conv_ch * config_.nb_erb / 2), 0.0f);
        result.e2.assign(static_cast<std::size_t>(config_.conv_ch * config_.nb_erb / 4), 0.0f);
        result.e3.assign(static_cast<std::size_t>(config_.conv_ch * config_.nb_erb / 4), 0.0f);
        result.emb.assign(static_cast<std::size_t>(config_.erb_hidden()), 0.0f);
        result.c0.assign(static_cast<std::size_t>(config_.conv_ch * config_.nb_df), 0.0f);
        for (std::size_t i = 0; i < result.emb.size(); ++i) {
            result.emb[i] = mean + static_cast<float>(i % 7) * 0.01f;
        }
        for (std::size_t i = 0; i < result.c0.size(); ++i) {
            result.c0[i] = mean * 0.5f + static_cast<float>(i % 5) * 0.02f;
        }
        // Keep the frame counter visible through the state only.
        result.e0[0] = counter;
        return result;
    }

    const std::vector<float>& run_erb_decoder(const enhancer::EncoderResult& encoder,
                                              enhancer::RecurrentState& state) override {
        (void)encoder;
        if (state.erb_decoder.empty()) {
            init_state(state);
        }
        state.erb_decoder[0][0] += 1.0f;
        mask_.assign(static_cast<std::size_t>(config_.nb_erb), 0.0f);
        for (std::size_t i = 0; i < mask_.size(); ++i) {
            mask_[i] = 0.5f + 0.001f * static_cast<float>(i) + 0.0001f * state.erb_decoder[0][0];
        }
        return mask_;
    }

    const std::vector<float>& run_df_decoder(const float* emb_window, const float* c0_window,
                                             enhancer::RecurrentState& state) override {
        if (state.df_decoder.empty()) {
            init_state(state);
        }
        state.df_decoder[0][0] += 1.0f;
        coefs_.assign(static_cast<std::size_t>(config_.df_order * config_.nb_df * 2), 0.0f);
        for (int k = 0; k < config_.df_order; ++k) {
            for (int f = 0; f < config_.nb_df; ++f) {
                const std::size_t base =
                    (static_cast<std::size_t>(k) * static_cast<std::size_t>(config_.nb_df) +
                     static_cast<std::size_t>(f)) *
                    2;
                coefs_[base] = 1.0f / static_cast<float>(k + 1);
                coefs_[base + 1] = 0.05f * static_cast<float>(f % 3);
            }
        }
        (void)emb_window;
        (void)c0_window;
        return coefs_;
    }

    float encoder_frame_count(const enhancer::RecurrentState& state) const {
        return state.encoder.empty() ? 0.0f : state.encoder[0][0];
    }

   private:
    enhancer::ModelConfig config_;
    std::vector<float> mask_;
    std::vector<float> coefs_;
};

}  // namespace dfn::test

#endif  // DFN_TEST_SUPPORT_STUB_RUNNER_H_
