#pragma once

#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <span>
#include "recorder_config.h"

class wave_file_writer final {
   public:
    wave_file_writer() = default;
    ~wave_file_writer() noexcept;

    wave_file_writer(const wave_file_writer&) = delete;
    wave_file_writer& operator=(const wave_file_writer&) = delete;

    wave_file_writer(wave_file_writer&&) = delete;
    wave_file_writer& operator=(wave_file_writer&&) = delete;

    void open(const std::filesystem::path& outputPath, const recorder_config& config);

    std::size_t write(std::span<const std::int16_t> samples);

    void finalize();

    [[nodiscard]] bool is_open() const noexcept;
    [[nodiscard]] std::uint64_t data_size() const noexcept;

   private:
    void write_header();
    void update_header();

    std::fstream _file;
    recorder_config config_{};
    std::uint64_t _blockSize = 0;
    bool _finalized = false;
};
