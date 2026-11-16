#pragma once

#include <climits>
#include <cstdint>
#include <stdexcept>

struct recorder_config final {
    std::uint32_t sampleRate = 48'000;
    std::uint16_t channelCount = 1;
    std::uint16_t bitsPerSample = 16;

    [[nodiscard]] std::uint16_t bytes_per_sample() const { return bitsPerSample / 8; }

    [[nodiscard]] std::uint16_t block_align() const {
        return static_cast<std::uint16_t>(channelCount * bytes_per_sample());
    }

    [[nodiscard]] std::uint32_t byteRate() const { return sampleRate * block_align(); }

    void validate() const {
        if (sampleRate == 0) throw std::invalid_argument("Sample rate must be greater than zero");
        if (channelCount == 0)
            throw std::invalid_argument("Channel count must be greater than zero");
        if (bitsPerSample != 16)
            throw std::invalid_argument("Only 16-bit PCM is currently supported");
        if (channelCount > UINT16_MAX / bytes_per_sample())
            throw std::invalid_argument("Channel count exceeds the PCM block-alignment limit");
        const auto calculatedByteRate =
            static_cast<std::uint64_t>(sampleRate) * channelCount * (bitsPerSample / 8);

        if (calculatedByteRate > UINT32_MAX)
            throw std::invalid_argument("WAV byte rate exceeds supported range");
    }
};
