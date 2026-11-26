#ifndef DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_
#define DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_

#include <cstddef>
#include <cstdint>
#include <deque>
#include <memory>
#include <mutex>
#include <span>
#include <string>
#include <vector>

#include "enhancer_session.h"
#include "model_config.h"
#include "onnx_backend.h"

namespace dfn::jni {

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

    // Returns true when a session was actually reset.
    bool reset();

    [[nodiscard]] int latency_samples();

    // Feeds one chunk and returns the emitted samples; finish() drains the tail.
    std::span<const float> stream_process(std::span<const float> samples,
                                          const enhance_options& options = {});

    std::span<const float> stream_finish(const enhance_options& options = {});

   private:
    // Per-stream state used by the blend.
    struct stream_state {
        bool finished = false;
        std::uint64_t consumed = 0;       // input samples fed
        std::uint64_t emitted = 0;        // samples emitted
        std::deque<float> input_history;  // input kept for the blend
        std::uint64_t history_start = 0;
        float speech_reference_db = -200.0f;
    };

    // Blends the emitted batch against the input. mutex_ held.
    void blend_emitted(std::vector<float>& batch, std::uint64_t emitted_before,
                       const enhance_options& options);

    // Drops input history the blend no longer needs. mutex_ held.
    void trim_history();

    [[nodiscard]] float input_at(std::uint64_t index) const;

    [[nodiscard]] bool validate_model_path() const;

    [[nodiscard]] const dfn::enhancer::model_config& config();
    void ensure_engine();

    std::recursive_mutex mutex_;

    std::string _model_dir;
    std::shared_ptr<const dfn::enhancer::model_config> _model_config;
    std::shared_ptr<dfn::enhancer::model_runner> _model_runner;
    std::shared_ptr<dfn::enhancer::EnhancerSession> _model_session;
    std::vector<float> _results;
    stream_state _stream;
};

}  // namespace dfn::jni

#endif  // DFN_ENHANCER_JNI_SPEECH_ENHANCER_MANAGER_H_
