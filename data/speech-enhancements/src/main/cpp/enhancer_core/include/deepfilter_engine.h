#ifndef DFN_ENHANCER_DEEPFILTER_ENGINE_H_
#define DFN_ENHANCER_DEEPFILTER_ENGINE_H_

// DeepFilterNet 3 enhancement pipeline for a single audio stream.
//
// One call consumes one hop of noisy audio and emits one hop of enhanced audio. The output
// is delayed by `ModelConfig::latency_samples()` (model lookahead plus the STFT window
// delay); callers that need sample-aligned output drop that many leading samples.
//
// The pipeline follows libDF/src/tract.rs:
//   1. STFT of the noisy hop and feature extraction (ERB + normalized complex spectrum).
//   2. Encoder inference -> local SNR, embedding, encoder taps, DF input features.
//   3. Stage selection from the local SNR (silence / bypass / ERB only / ERB + DF).
//   4. ERB mask decoding and/or deep filtering on the spectrum of frame t - lookahead.
//   5. Optional post filter and attenuation limit.
//   6. Inverse STFT with overlap-add.

#include <cstddef>
#include <memory>
#include <span>
#include <vector>

#include "enhancer_config.h"
#include "enhancer_state.h"
#include "model_config.h"
#include "onnx_backend.h"

namespace dfn::enhancer {

class DeepFilterEngine {
   public:
    DeepFilterEngine(model_config config, std::shared_ptr<model_runner> runner,
                     EnhancerOptions options = {});

    [[nodiscard]] const model_config& config() const noexcept { return state_.config; }
    [[nodiscard]] const EnhancerState& state() const noexcept { return state_; }

    void reset();

    // Consumes `hop_size` noisy samples and writes `hop_size` enhanced samples.
    void process_frame(std::span<const float> noisy, std::span<float> enhanced);

    [[nodiscard]] constexpr float last_lsnr() const noexcept { return last_lsnr_; }
    [[nodiscard]] constexpr EnhancementStage last_stage() const noexcept { return last_stage_; }

    void set_post_filter_beta(float beta) noexcept;
    void set_attenuation_limit_db(float db) noexcept;
    [[nodiscard]] constexpr float post_filter_beta() const noexcept {
        return options_.post_filter_beta;
    }

    // Selects the processing stage for a local SNR estimate (exposed for tests).
    [[nodiscard]] EnhancementStage select_stage(float lsnr) const noexcept;

   private:
    void push_feature_windows(std::span<const float> erb_feature,
                              std::span<const float> complex_feature);
    void push_decoder_windows(const encoder_result& result);
    void gather_df_history();

    EnhancerState state_;
    std::shared_ptr<model_runner> runner_;
    EnhancerOptions options_;
    std::vector<int> erb_widths_;
    std::vector<float> complex_feature_scratch_;
    float last_lsnr_ = 0.0f;
    EnhancementStage last_stage_ = EnhancementStage::kBypass;
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_DEEPFILTER_ENGINE_H_
