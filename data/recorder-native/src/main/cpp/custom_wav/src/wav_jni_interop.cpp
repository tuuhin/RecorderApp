#include <jni.h>

#include "jni_utils.h"
#include "wav_recorder.h"

#include <android/log.h>

#include <cstdarg>
#include <exception>
#include <filesystem>
#include <memory>
#include <stdexcept>
#include <string>
#include <utility>

namespace {

#define LOG_TAG "WAV_JNI_INTEROP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

void throwJavaException(JNIEnv* env, const char* className, const char* message) noexcept {
    if (env->ExceptionCheck()) return;

    jclass exceptionClass = env->FindClass(className);

    if (exceptionClass == nullptr) return;

    env->ThrowNew(exceptionClass, message);
    env->DeleteLocalRef(exceptionClass);
}

void translateCurrentException(JNIEnv* env) noexcept {
    try {
        throw;
    } catch (const std::invalid_argument& error) {
        LOGD("native failure: invalid_argument: %s", error.what());
        throwJavaException(env, "java/lang/IllegalArgumentException", error.what());
    } catch (const std::logic_error& error) {
        LOGD("native failure: logic_error: %s", error.what());
        throwJavaException(env, "java/lang/IllegalStateException", error.what());
    } catch (const std::exception& error) {
        LOGD("native failure: exception: %s", error.what());
        throwJavaException(env, "java/lang/RuntimeException", error.what());
    } catch (...) {
        LOGD("native failure: unknown exception");
        throwJavaException(env, "java/lang/Exception", "Unknown native recorder error");
    }
}

wav_recorder* get_recorder(jlong handle) {
    if (handle == 0) throw std::invalid_argument("Native recorder handle is null");
    return reinterpret_cast<wav_recorder*>(static_cast<std::uintptr_t>(handle));
}

template <typename Fn>
void withRecorder(JNIEnv* env, jlong handle, Fn&& function) noexcept {
    try {
        auto* recorder = get_recorder(handle);
        std::forward<Fn>(function)(*recorder);
    } catch (...) {
        translateCurrentException(env);
    }
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_createRecorder(JNIEnv* env, jobject) {
    try {
        auto recorder = std::make_unique<wav_recorder>();

        const auto address = reinterpret_cast<std::uintptr_t>(recorder.release());

        const auto printableAddress = static_cast<unsigned long long>(address);

        LOGD("createRecorder: allocated wav_recorder, pointer=0x%llx, top-byte tag=0x%02llx",
             printableAddress,
             (printableAddress >> 56U) & 0xFFULL);

        return static_cast<jlong>(address);
    } catch (...) {
        LOGD("createRecorder: allocation failed, returning 0");
        translateCurrentException(env);
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_releaseNative(JNIEnv* env, jobject, jlong handle) {
    if (handle == 0) {
        LOGD("releaseNative: null handle, nothing to release");
        return;
    }

    LOGD("releaseNative: releasing handle %lld (0x%llx)",
         static_cast<long long>(handle),
         static_cast<unsigned long long>(handle));

    try {
        std::unique_ptr<wav_recorder> recorder(get_recorder(handle));
        recorder->release();
    } catch (...) {
        translateCurrentException(env);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_eva_recorder_1native_WavAudioRecorder_setConfig(
    JNIEnv* env, jobject, jlong handle, jint sample_rate, jint channels) {
    withRecorder(env, handle, [&](wav_recorder& recorder) {
        LOGD("setConfig: sampleRate=%d, channels=%d", sample_rate, channels);

        if (sample_rate <= 0 || channels <= 0)
            throw std::invalid_argument("Sample rate and channel count must be positive");

        recorder_config config{};
        config.sampleRate = static_cast<std::uint32_t>(sample_rate);
        config.channelCount = static_cast<std::uint16_t>(channels);
        config.bitsPerSample = 16;

        if (channels > UINT16_MAX)
            throw std::invalid_argument("Channel count exceeds supported range");

        config.validate();
        recorder.setConfig(config);
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_setOutputFileConfig(JNIEnv* env, jobject,
                                                                   jlong handle,
                                                                   jstring file_path) {
    try {
        UtfChars path(env, file_path);

        LOGD("setOutputFileConfig: path=%s", path.get());

        withRecorder(env, handle, [&](wav_recorder& recorder) {
            recorder.setOutputFile(std::filesystem::path(path.get()));
        });
    } catch (...) {
        translateCurrentException(env);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_prepareRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("prepareRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.prepare(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_startRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("startRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.start(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_stopRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("stopRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.stop(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_pauseRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("pauseRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.pause(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_resumeRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("resumeRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.resume(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_resetRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("resetRecorder: handle %lld", static_cast<long long>(handle));
    withRecorder(env, handle, [](wav_recorder& recorder) { recorder.reset(); });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_isRecordRunning(JNIEnv* env, jobject, jlong handle) {
    jboolean result = JNI_FALSE;

    withRecorder(env, handle, [&](wav_recorder& recorder) {
        result = recorder.is_recording() ? JNI_TRUE : JNI_FALSE;
    });

    LOGD("isRecordRunning: %s", result == JNI_TRUE ? "true" : "false");

    return result;
}
