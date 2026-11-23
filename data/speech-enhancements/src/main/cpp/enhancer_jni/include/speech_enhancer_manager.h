#ifndef DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_
#define DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_

#include <cstddef>
#include <memory>
#include <mutex>
#include <span>
#include <string>
#include <vector>

#include "enhancer_session.h"
#include "model_config.h"
#include "onnx_backend.h"

namespace dfn::jni {

// v1 of the engine processes 48 kHz mono only; resampling in and out is a later phase.
inline constexpr int kModelSampleRate = 48000;

class speech_enhancer_manager {
   public:
    explicit speech_enhancer_manager(std::string modelDir);

    speech_enhancer_manager(const speech_enhancer_manager&) = delete;
    speech_enhancer_manager& operator=(const speech_enhancer_manager&) = delete;

    bool prepare();
    int latency_samples();

    std::span<const float> enhance(std::span<const float> samples, int sample_rate, float strength);

   private:
    [[nodiscard]] bool validate_model_path() const;

    const dfn::enhancer::ModelConfig& _config();
    void _ensure_engine();

    std::recursive_mutex mutex_;

    std::string model_dir_;
    std::shared_ptr<const dfn::enhancer::ModelConfig> _model_config;
    std::shared_ptr<dfn::enhancer::ModelRunner> _model_runner;
    std::shared_ptr<dfn::enhancer::EnhancerSession> _model_session;
    std::vector<float> output_;
};

}  // namespace dfn::jni

#endif  // DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_
