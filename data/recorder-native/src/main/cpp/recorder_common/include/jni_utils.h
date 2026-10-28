#pragma once

#include <android/log.h>
#include <jni.h>
#include <cstdint>
#include <exception>
#include <stdexcept>
#include <utility>

#define LOG_TAG_I "RECORDER_JNI_COMMON"
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_I, __VA_ARGS__)

namespace recorder_jni {

inline void throwJavaException(JNIEnv* env, const char* className, const char* message) noexcept {
    if (env->ExceptionCheck()) return;

    jclass exceptionClass = env->FindClass(className);

    if (exceptionClass == nullptr) return;

    env->ThrowNew(exceptionClass, message);
    env->DeleteLocalRef(exceptionClass);
}

inline void translateCurrentException(JNIEnv* env) noexcept {
    try {
        throw;
    } catch (const std::invalid_argument& error) {
        LOG_DEBUG("native failure: invalid_argument: %s", error.what());
        throwJavaException(env, "java/lang/IllegalArgumentException", error.what());
    } catch (const std::logic_error& error) {
        LOG_DEBUG("native failure: logic_error: %s", error.what());
        throwJavaException(env, "java/lang/IllegalStateException", error.what());
    } catch (const std::exception& error) {
        LOG_DEBUG("native failure: exception: %s", error.what());
        throwJavaException(env, "java/lang/RuntimeException", error.what());
    } catch (...) {
        LOG_DEBUG("native failure: unknown exception");
        throwJavaException(env, "java/lang/Exception", "Unknown native recorder error");
    }
}

template <typename Recorder>
Recorder* get_recorder(jlong handle) {
    if (handle == 0) throw std::invalid_argument("Native recorder handle is null");
    return reinterpret_cast<Recorder*>(static_cast<std::uintptr_t>(handle));
}

template <typename Recorder, typename Fn>
void withRecorder(JNIEnv* env, jlong handle, Fn&& function) noexcept {
    try {
        auto* recorder = get_recorder<Recorder>(handle);
        std::forward<Fn>(function)(*recorder);
    } catch (...) {
        translateCurrentException(env);
    }
}

}  // namespace recorder_jni

class UtfChars final {
   public:
    UtfChars(JNIEnv* env, jstring value) : _jEnv(env), _jValue(value) {
        if (_jValue == nullptr) throw std::invalid_argument("File path must not be null");
        _cChars = _jEnv->GetStringUTFChars(_jValue, nullptr);
        if (_cChars == nullptr) throw std::runtime_error("Failed to access Java string");
    }

    ~UtfChars() {
        if (_cChars != nullptr) _jEnv->ReleaseStringUTFChars(_jValue, _cChars);
    }

    UtfChars(const UtfChars&) = delete;
    UtfChars& operator=(const UtfChars&) = delete;

    [[nodiscard]] const char* get() const noexcept { return _cChars; }

   private:
    JNIEnv* _jEnv;
    jstring _jValue;
    const char* _cChars = nullptr;
};
