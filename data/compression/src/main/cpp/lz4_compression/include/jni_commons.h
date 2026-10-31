#pragma once

#include <android/log.h>
#include <jni.h>

#include <exception>
#include <stdexcept>
#include <utility>

#define LOG_TAG_I "NATIVE_COMPRESSION"
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_I, __VA_ARGS__)

namespace jni_commons {

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
        throwJavaException(env, "java/lang/Exception", "Unknown native compression error");
    }
}

template <typename Fn> jint withExceptions(JNIEnv* env, jint failureCode, Fn&& function) noexcept {
    try {
        return std::forward<Fn>(function)();
    } catch (...) {
        translateCurrentException(env);
        return failureCode;
    }
}
} // namespace jni_commons
