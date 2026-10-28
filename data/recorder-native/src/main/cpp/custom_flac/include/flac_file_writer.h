#pragma once

#include "recorder_config.h"

#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <span>
#include <vector>

#include "FLAC/stream_encoder.h"

class flac_file_writer final {
   public:
    flac_file_writer() = default;
    ~flac_file_writer() noexcept;

    flac_file_writer(const flac_file_writer&) = delete;
    flac_file_writer& operator=(const flac_file_writer&) = delete;

    flac_file_writer(flac_file_writer&&) = delete;
    flac_file_writer& operator=(flac_file_writer&&) = delete;

    void open(const std::filesystem::path& outputPath, const recorder_config& config);

    std::size_t write(std::span<const std::int16_t> samples);

    void finalize();

    [[nodiscard]] bool is_open() const noexcept;

    [[nodiscard]]
    std::uint64_t frames_written() const noexcept;

   private:
    FLAC__StreamEncoder* _encoder = nullptr;
    recorder_config config_{};
    std::vector<FLAC__int32> _sample_buffer;
    std::uint64_t _frames_written = 0;
    bool _finalized = false;
};
