#include <jni.h>

#include <memory>
#include <span>
#include <string>

#include "jni_utils.h"
#include "speech_enhancer_manager.h"

using speech_jni::destroy_handle;
using speech_jni::make_handle;
using speech_jni::read_options;
using speech_jni::read_sample_buffer;
using speech_jni::translateCurrentException;
using speech_jni::withManager;
using speech_jni::wrap_result;

using SEM = dfn::jni::speech_enhancer_manager;

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
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_processNative(
    JNIEnv* env, jobject, jlong handle, jobject samples, jfloat strength,
    jboolean adaptive_strength, jfloat post_filter_beta, jfloat attenuation_limit_db) {
    jobject result = nullptr;

    LOG_DEBUG(
        "processNative: handle=%lld, strength=%.3f, adaptive=%d, "
        "post_filter_beta=%.4f, attenuation_limit_db=%.1f",
        static_cast<long long>(handle),
        static_cast<float>(strength),
        adaptive_strength == JNI_TRUE ? 1 : 0,
        static_cast<float>(post_filter_beta),
        static_cast<float>(attenuation_limit_db));

    withManager<SEM>(env, handle, [&](SEM& manager) {
        const std::span<const float> input_samples = read_sample_buffer(env, samples);
        const dfn::jni::enhance_options options =
            read_options(strength, adaptive_strength, post_filter_beta, attenuation_limit_db);

        const std::span<const float> emitted = manager.stream_process(input_samples, options);
        result = wrap_result(env, emitted);
    });

    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_eva_speech_1enhancements_ClearSpeechEnhancer_finishNative(JNIEnv* env, jobject,
                                                                   jlong handle, jfloat strength,
                                                                   jboolean adaptive_strength,
                                                                   jfloat post_filter_beta,
                                                                   jfloat attenuation_limit_db) {
    jobject result = nullptr;

    LOG_DEBUG(
        "finishNative: handle=%lld, strength=%.3f, adaptive=%d, "
        "post_filter_beta=%.4f, attenuation_limit_db=%.1f",
        static_cast<long long>(handle),
        static_cast<float>(strength),
        adaptive_strength == JNI_TRUE ? 1 : 0,
        static_cast<float>(post_filter_beta),
        static_cast<float>(attenuation_limit_db));

    withManager<SEM>(env, handle, [&](SEM& manager) {
        const dfn::jni::enhance_options options =
            read_options(strength, adaptive_strength, post_filter_beta, attenuation_limit_db);

        const std::span<const float> emitted = manager.stream_finish(options);
        result = wrap_result(env, emitted);
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
