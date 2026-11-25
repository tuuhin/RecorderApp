#pragma once

#include <android/log.h>
#include <jni.h>

#include <concepts>
#include <cstdint>
#include <exception>
#include <memory>
#include <stdexcept>
#include <string_view>
#include <utility>

#include "handle_registry_manager.h"

inline constexpr std::string_view kLogTag = "SPEECH_JNI_COMMON";

#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, kLogTag.data(), __VA_ARGS__)

namespace speech_jni {

inline void throwJavaException(JNIEnv* env, const char* class_name, const char* message) noexcept {
    if (env == nullptr || env->ExceptionCheck()) return;

    jclass exception_class = env->FindClass(class_name);
    if (exception_class == nullptr) return;

    env->ThrowNew(exception_class, message);
    env->DeleteLocalRef(exception_class);
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

// Opaque native handles for the speech enhancer. Handles are sequential ids
// owned by the shared handle_registry_manager, never raw pointer addresses:
// a stale, reused or forged handle resolves to nothing instead of being
// reinterpreted as a pointer (type confusion / use-after-free).
template <typename SM>
[[nodiscard]] jlong make_handle(std::shared_ptr<SM> manager) {
    const auto handle = handle_registry_manager<SM>::instance().register_handle(std::move(manager));
    if (handle == handle_registry_manager<SM>::kInvalidHandle)
        throw std::runtime_error("Failed to register the native speech enhancer");
    return static_cast<jlong>(handle);
}

// Owning copy so the manager stays alive for the whole call even if the handle
// is destroyed concurrently.
template <typename SM>
[[nodiscard]] std::shared_ptr<SM> get_manager(jlong handle) {
    if (handle == 0) throw std::invalid_argument("Native speech enhancer handle is null");

    auto manager = handle_registry_manager<SM>::instance().from_handle(handle);
    if (!manager) throw std::invalid_argument("Native speech enhancer handle is not valid");

    return manager;
}

template <typename SM>
void destroy_handle(jlong handle) noexcept {
    if (handle == 0) return;
    // Registry entry is erased under the lock; the returned shared_ptr keeps the
    // destructor outside it and is dropped here.
    (void)handle_registry_manager<SM>::instance().destroy_handle(handle);
}

template <typename SM, typename Fn>
    requires std::invocable<Fn&&, SM&>
void withManager(JNIEnv* env, jlong handle, Fn&& function) noexcept {
    try {
        auto manager = get_manager<SM>(handle);
        std::forward<Fn>(function)(*manager);
    } catch (...) {
        translateCurrentException(env);
    }
}
}  // namespace speech_jni

class UtfChars final {
   public:
    UtfChars(JNIEnv* env, jstring value) : _env(env), _jValue(value) {
        if (_env == nullptr) throw std::invalid_argument("JNIEnv must not be null");
        if (_jValue == nullptr) throw std::invalid_argument("Model directory must not be null");

        _chars = _env->GetStringUTFChars(_jValue, nullptr);
        if (_chars == nullptr) throw std::runtime_error("Failed to access Java string");
    }

    ~UtfChars() {
        if (_chars != nullptr && _env != nullptr) _env->ReleaseStringUTFChars(_jValue, _chars);
    }

    UtfChars(const UtfChars&) = delete;
    UtfChars& operator=(const UtfChars&) = delete;
    UtfChars(UtfChars&&) = delete;
    UtfChars& operator=(UtfChars&&) = delete;

    [[nodiscard]] const char* get() const noexcept { return _chars; }
    [[nodiscard]] std::string_view view() const noexcept {
        return _chars != nullptr ? std::string_view{_chars} : std::string_view{};
    }

   private:
    JNIEnv* _env = nullptr;
    jstring _jValue = nullptr;
    const char* _chars = nullptr;
};
