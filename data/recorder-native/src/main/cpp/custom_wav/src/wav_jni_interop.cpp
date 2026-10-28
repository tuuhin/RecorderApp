#include <jni.h>

#include "audio_recorder.h"
#include "jni_utils.h"
#include "wav_file_writer.h"

#include <android/log.h>
#include <cstdint>
#include <filesystem>
#include <memory>
#include <stdexcept>

namespace {

#define LOG_TAG "WAV_JNI_INTEROP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using wav_recorder = audio_recorder<wave_file_writer>;

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
        recorder_jni::translateCurrentException(env);
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
        std::unique_ptr<wav_recorder> recorder(recorder_jni::get_recorder<wav_recorder>(handle));
        recorder->release();
    } catch (...) {
        recorder_jni::translateCurrentException(env);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_eva_recorder_1native_WavAudioRecorder_setConfig(
    JNIEnv* env, jobject, jlong handle, jint sample_rate, jint channels) {
    recorder_jni::withRecorder<wav_recorder>(env, handle, [&](wav_recorder& recorder) {
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

        recorder_jni::withRecorder<wav_recorder>(env, handle, [&](wav_recorder& recorder) {
            recorder.setOutputFile(std::filesystem::path(path.get()));
        });
    } catch (...) {
        recorder_jni::translateCurrentException(env);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_prepareRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("prepareRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.prepare(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_startRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("startRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.start(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_stopRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("stopRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.stop(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_pauseRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("pauseRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.pause(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_resumeRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("resumeRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.resume(); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_resetRecorder(JNIEnv* env, jobject, jlong handle) {
    LOGD("resetRecorder: handle %lld", static_cast<long long>(handle));
    recorder_jni::withRecorder<wav_recorder>(
        env, handle, [](wav_recorder& recorder) { recorder.reset(); });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eva_recorder_1native_WavAudioRecorder_isRecordRunning(JNIEnv* env, jobject, jlong handle) {
    jboolean result = JNI_FALSE;

    recorder_jni::withRecorder<wav_recorder>(env, handle, [&](wav_recorder& recorder) {
        result = recorder.is_recording() ? JNI_TRUE : JNI_FALSE;
    });

    LOGD("isRecordRunning: %s", result == JNI_TRUE ? "true" : "false");

    return result;
}
