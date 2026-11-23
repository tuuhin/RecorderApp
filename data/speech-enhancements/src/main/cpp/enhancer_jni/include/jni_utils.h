#pragma once

#include <android/log.h>
#include <jni.h>
#include <cstdint>
#include <exception>
#include <memory>
#include <stdexcept>
#include <utility>

#define LOG_TAG_I "SPEECH_JNI_COMMON"
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_I, __VA_ARGS__)

namespace speech_jni {

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
        throwJavaException(env, "java/lang/Exception", "Unknown native speech enhancer error");
    }
}

template <typename SM>
std::shared_ptr<SM>& get_manager(jlong handle) {
    if (handle == 0) throw std::invalid_argument("Native speech enhancer handle is null");
    return *reinterpret_cast<std::shared_ptr<SM>*>(static_cast<std::uintptr_t>(handle));
}

template <typename Manager>
jlong make_handle(std::shared_ptr<Manager> manager) {
    auto* owned = new std::shared_ptr<Manager>(std::move(manager));
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(owned));
}

template <typename SM>
void destroy_handle(jlong handle) noexcept {
    if (handle == 0) return;
    delete reinterpret_cast<std::shared_ptr<SM>*>(static_cast<std::uintptr_t>(handle));
}

template <typename SM, typename Fn>
void withManager(JNIEnv* env, jlong handle, Fn&& function) noexcept {
    try {
        auto& manager = get_manager<SM>(handle);
        std::forward<Fn>(function)(*manager);
    } catch (...) {
        translateCurrentException(env);
    }
}
}  // namespace speech_jni

class UtfChars final {
   public:
    UtfChars(JNIEnv* env, jstring value) : _jEnv(env), _jValue(value) {
        if (_jValue == nullptr) throw std::invalid_argument("Model directory must not be null");
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
