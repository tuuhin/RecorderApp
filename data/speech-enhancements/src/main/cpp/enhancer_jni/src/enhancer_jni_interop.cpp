#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <memory>
#include <span>
#include <stdexcept>
#include <string>
#include <utility>

#include "jni_utils.h"
#include "speech_enhancer_manager.h"

namespace {

using speech_jni::destroy_handle;
using speech_jni::make_handle;
using speech_jni::translateCurrentException;
using speech_jni::withManager;

using SEM = dfn::jni::speech_enhancer_manager;

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_createNative(JNIEnv* env, jobject,
                                                                   jstring model_dir) {
    try {
        const UtfChars directory(env, model_dir);
        LOG_DEBUG("createNative: model_dir=%s", directory.get());
        return make_handle<SEM>(std::make_shared<SEM>(std::string{directory.view()}));
    } catch (...) {
        translateCurrentException(env);
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_destroyNative(JNIEnv*, jobject,
                                                                    jlong handle) noexcept {
    LOG_DEBUG("destroyNative: handle=%lld", static_cast<long long>(handle));
    destroy_handle<SEM>(handle);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_prepareNative(JNIEnv* env, jobject,
                                                                    jlong handle) {
    jboolean result = JNI_FALSE;

    withManager<SEM>(env, handle, [&](SEM& manager) {
        LOG_DEBUG("prepareNative: handle=%lld", static_cast<long long>(handle));
        result = manager.prepare() ? JNI_TRUE : JNI_FALSE;
    });

    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_resetNative(JNIEnv* env, jobject,
                                                                  jlong handle) {
    jboolean result = JNI_FALSE;

    withManager<SEM>(env, handle, [&](SEM& manager) {
        LOG_DEBUG("resetNative: handle=%lld", static_cast<long long>(handle));
        result = manager.reset() ? JNI_TRUE : JNI_FALSE;
    });

    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_enhanceNative(
    JNIEnv* env, jobject, jlong handle, jobject samples, jint sample_rate, jfloat strength,
    jboolean adaptive_strength, jfloat post_filter_beta, jfloat attenuation_limit_db) {
    jobject result = nullptr;

    LOG_DEBUG(
        "enhanceNative: handle=%lld, sample_rate=%d, strength=%.3f, adaptive=%d, "
        "post_filter_beta=%.4f, attenuation_limit_db=%.1f",
        static_cast<long long>(handle),
        static_cast<int>(sample_rate),
        static_cast<float>(strength),
        adaptive_strength == JNI_TRUE ? 1 : 0,
        static_cast<float>(post_filter_beta),
        static_cast<float>(attenuation_limit_db));

    withManager<SEM>(env, handle, [&](SEM& manager) {
        if (samples == nullptr) throw std::invalid_argument("samples must not be null");

        const auto* address = static_cast<const float*>(env->GetDirectBufferAddress(samples));
        const jlong capacity = env->GetDirectBufferCapacity(samples);

        if (address == nullptr || capacity <= 0 ||
            capacity % static_cast<jlong>(sizeof(float)) != 0)
            throw std::invalid_argument("samples must be a direct ByteBuffer of floats");

        const auto count = static_cast<std::size_t>(capacity) / sizeof(float);
        const std::span<const float> input_samples{address, count};

        dfn::jni::enhance_options options;
        options.strength = static_cast<float>(strength);
        options.adaptive_strength = adaptive_strength == JNI_TRUE;
        options.post_filter_beta = static_cast<float>(post_filter_beta);
        options.attenuation_limit_db = static_cast<float>(attenuation_limit_db);

        const std::span<const float> enhanced =
            manager.enhance(input_samples, static_cast<int>(sample_rate), options);

        // A zero-copy view of the manager's retained output: native-endian, position 0,
        // limit = byte count. Valid until the next enhanceNative or destroyNative.
        result = env->NewDirectByteBuffer(const_cast<float*>(enhanced.data()),
                                          static_cast<jlong>(enhanced.size_bytes()));
    });

    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_nativeLatencySamples(JNIEnv* env, jobject,
                                                                           jlong handle) {
    jint result = 0;

    withManager<SEM>(
        env, handle, [&](SEM& manager) { result = static_cast<jint>(manager.latency_samples()); });

    return result;
}
