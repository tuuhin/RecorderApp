
#include "lz4_compressor.h"
#include "decompression_ctx.h"

#include "android/log.h"
#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <stdexcept>
#include <vector>

#include <jni.h>
#include <lz4frame.h>

#define LOG_TAG "LZ4_JNI_INTEROP"
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

std::vector<std::uint8_t> lz4_compressor::compress_frame(const std::span<const std::uint8_t> source) {
    const std::size_t capacity = LZ4F_compressFrameBound(source.size(), nullptr);

    std::vector<std::uint8_t> destination(capacity);

    auto written = LZ4F_compressFrame(destination.data(), destination.size(), source.data(), source.size(), nullptr);

    if (LZ4F_isError(written)) {
        LOG_D("compressFrame: lz4 failure: %s", LZ4F_getErrorName(written));
        throw std::runtime_error("LZ4 frame compression failed");
    }

    destination.resize(written);
    return destination;
}

std::vector<std::uint8_t> lz4_compressor::decompress_frame(const std::span<const std::uint8_t> source) {

    if (source.empty()) throw std::runtime_error("Cannot decompress an empty LZ4 frame");

    de_compression_context context;

    LZ4F_frameInfo_t frameInfo{};
    std::size_t headerInputSize = source.size();

    const auto headerResult = LZ4F_getFrameInfo(context.get(), &frameInfo, source.data(), &headerInputSize);

    if (LZ4F_isError(headerResult)) {
        LOG_D("decompressFrame: invalid or incomplete LZ4 frame header: %s", LZ4F_getErrorName(headerResult));
        throw std::runtime_error("Input is not a valid LZ4 frame");
    }

    // Otherwise, grow the buffer incrementally.
    std::size_t capacity = frameInfo.contentSize;

    // outgrows the limit
    if (capacity > lz4::kMaxGrownOutputBytes)
        throw std::runtime_error("Decompressed output exceeds the supported size");

    if (capacity == 0) {
        capacity = std::min(std::max<std::size_t>(source.size() * 2, 64), lz4::kMaxGrownOutputBytes);
    }

    std::vector<std::uint8_t> destination(capacity);

    std::size_t inputOffset  = headerInputSize;
    std::size_t outputOffset = 0;

    for (;;) {
        if (outputOffset == destination.size()) {
            if (destination.size() >= lz4::kMaxGrownOutputBytes)
                throw std::runtime_error("Decompressed output exceeds the supported size");

            const std::size_t newCapacity = std::min(destination.size() * 2, lz4::kMaxGrownOutputBytes);

            destination.resize(newCapacity);
        }

        std::size_t inputSize  = source.size() - inputOffset;
        std::size_t outputSize = destination.size() - outputOffset;

        const std::size_t result = LZ4F_decompress(context.get(), destination.data() + outputOffset, &outputSize,
                                                   source.data() + inputOffset, &inputSize, nullptr);

        if (LZ4F_isError(result)) {
            LOG_D("decompressFrame: lz4 failure: %s", LZ4F_getErrorName(result));
            throw std::runtime_error("LZ4 frame decompression failed");
        }

        inputOffset += inputSize;
        outputOffset += outputSize;

        if (result == 0) {
            if (inputOffset != source.size()) throw std::runtime_error("Unexpected trailing data after LZ4 frame");
            destination.resize(outputOffset);
            return destination;
        }

        if (inputSize == 0 && outputSize == 0) throw std::runtime_error("LZ4 decompression made no progress");

        if (inputOffset == source.size()) throw std::runtime_error("Incomplete LZ4 frame");
    }
}
