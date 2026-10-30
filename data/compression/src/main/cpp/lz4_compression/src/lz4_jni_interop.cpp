#include <jni.h>

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <exception>
#include <span>
#include <vector>

#include "jni_commons.h"
#include "lz4_compressor.h"

namespace {

constexpr jint kInvalidBuffer        = -1;
constexpr jint kInsufficientCapacity = -2;
constexpr jint kCompressionFailure   = -3;

struct buffer_window {
    std::uint8_t* address;
    std::size_t remaining;
};

bool get_buffer_window(JNIEnv* env, jobject buffer, buffer_window& window) {
    if (buffer == nullptr) return false;

    auto* address = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(buffer));

    const jlong capacity = env->GetDirectBufferCapacity(buffer);

    if (address == nullptr || capacity < 0) return false;

    jclass klass = env->GetObjectClass(buffer);

    if (klass == nullptr) return false;

    const auto position_method = env->GetMethodID(klass, "position", "()I");

    const auto limit_method = env->GetMethodID(klass, "limit", "()I");

    if (position_method == nullptr || limit_method == nullptr) {
        env->DeleteLocalRef(klass);
        return false;
    }

    const jint position = env->CallIntMethod(buffer, position_method);
    const jint limit    = env->CallIntMethod(buffer, limit_method);

    env->DeleteLocalRef(klass);

    if (env->ExceptionCheck()) return false;

    if (position < 0 || limit < position || limit > capacity) return false;

    window.address   = address + position;
    window.remaining = static_cast<std::size_t>(limit - position);
    return true;
}

} // namespace

extern "C" JNIEXPORT jint JNICALL Java_com_eva_compression_LZ4NativeCompression_compressBytes(JNIEnv* env,
                                                                                              jobject /* thiz */,
                                                                                              jobject input,
                                                                                              jobject output) {

    buffer_window inputWindow{};
    buffer_window outputWindow{};

    if (!get_buffer_window(env, input, inputWindow) || !get_buffer_window(env, output, outputWindow))
        return kInvalidBuffer;

    jni_commons::withExceptions(env, [inputWindow, outputWindow]() {
        const std::span<const std::uint8_t> source(inputWindow.address, inputWindow.remaining);

        const std::vector<std::uint8_t> compressed = lz4_compressor::compress_frame(source);

        if (compressed.size() > outputWindow.remaining) return kInsufficientCapacity;

        if (!compressed.empty()) std::memcpy(outputWindow.address, compressed.data(), compressed.size());

        return static_cast<jint>(compressed.size());
    });

    return kCompressionFailure;
}

extern "C" JNIEXPORT jint JNICALL Java_com_eva_compression_LZ4NativeCompression_decompressBytes(JNIEnv* env,
                                                                                                jobject /* thiz */,
                                                                                                jobject input,
                                                                                                jobject output) {
    buffer_window inputWindow{};
    buffer_window outputWindow{};

    if (!get_buffer_window(env, input, inputWindow) || !get_buffer_window(env, output, outputWindow))
        return kInvalidBuffer;

    jni_commons::withExceptions(env, [inputWindow, outputWindow]() {
        const std::span<const std::uint8_t> source(inputWindow.address, inputWindow.remaining);

        const std::vector<std::uint8_t> decompressed = lz4_compressor::decompress_frame(source);

        if (decompressed.size() > outputWindow.remaining) return kInsufficientCapacity;

        if (!decompressed.empty()) std::memcpy(outputWindow.address, decompressed.data(), decompressed.size());

        return static_cast<jint>(decompressed.size());
    });

    return kCompressionFailure;
}
