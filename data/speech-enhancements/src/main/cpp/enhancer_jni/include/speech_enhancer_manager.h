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

inline constexpr int kModelSampleRate = 48000;

struct enhance_options {
    float strength = 1.0f;
    bool adaptive_strength = false;
    float post_filter_beta = 0.0f;
    float attenuation_limit_db = 100.0f;
};

class speech_enhancer_manager {
   public:
    explicit speech_enhancer_manager(std::string model_dir);

    speech_enhancer_manager(const speech_enhancer_manager&) = delete;
    speech_enhancer_manager& operator=(const speech_enhancer_manager&) = delete;

    bool prepare();

    // Restores the streaming session to a clean state. Returns true when a session
    // existed and was actually reset, false when there was nothing to reset.
    bool reset();

    [[nodiscard]] int latency_samples();

    [[nodiscard]] std::span<const float> enhance(std::span<const float> samples, int sample_rate,
                                                 const enhance_options& options = {});

   private:
    [[nodiscard]] bool validate_model_path() const;

    [[nodiscard]] const dfn::enhancer::model_config& config();
    void ensure_engine();

    std::recursive_mutex mutex_;

    std::string _model_dir;
    std::shared_ptr<const dfn::enhancer::model_config> _model_config;
    std::shared_ptr<dfn::enhancer::model_runner> _model_runner;
    std::shared_ptr<dfn::enhancer::EnhancerSession> _model_session;
    std::vector<float> _results;
};

}  // namespace dfn::jni

#endif  // DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_
