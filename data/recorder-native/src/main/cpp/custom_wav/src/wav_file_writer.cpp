#include <array>
#include <cstddef>
#include <cstdio>
#include <limits>
#include <stdexcept>
#include <string>
#include <system_error>

#include "recorder_config.h"
#include "wav_file_writer.h"

namespace {

constexpr std::uint32_t RIFF_HEADER_SIZE = 36;
constexpr std::uint32_t WAV_HEADER_SIZE = 44;

// RIFF stores its chunk size in a 32-bit unsigned field.
constexpr std::uint64_t MAX_RIFF_DATA_SIZE =
    std::numeric_limits<std::uint32_t>::max() - RIFF_HEADER_SIZE;

void writeBytes(std::FILE* file, const void* data, std::size_t size) {
    if (std::fwrite(data, 1, size, file) != size)
        throw std::runtime_error("Failed to write WAV file");
}

void writeU16LE(std::FILE* file, std::uint16_t value) {
    const std::array<char, 2> bytes{static_cast<char>(value & 0xFF),
                                    static_cast<char>((value >> 8) & 0xFF)};

    writeBytes(file, bytes.data(), bytes.size());
}

void writeU32LE(std::FILE* file, std::uint32_t value) {
    const std::array<char, 4> bytes{static_cast<char>(value & 0xFF),
                                    static_cast<char>((value >> 8) & 0xFF),
                                    static_cast<char>((value >> 16) & 0xFF),
                                    static_cast<char>((value >> 24) & 0xFF)};

    writeBytes(file, bytes.data(), bytes.size());
}

void writeFourCC(std::FILE* file, const char (&id)[5]) {
    writeBytes(file, id, 4);
}

void seekTo(std::FILE* file, std::uint32_t offset) {
    if (std::fseek(file, static_cast<long>(offset), SEEK_SET) != 0)
        throw std::runtime_error("Failed to position WAV data stream");
}

}  // namespace

wave_file_writer::~wave_file_writer() noexcept {
    try {
        abort();
    } catch (...) {
        if (_file != nullptr) {
            std::fclose(_file);
            _file = nullptr;
        }
    }
}

void wave_file_writer::open(const std::filesystem::path& outputPath,
                            const recorder_config& config) {
    if (is_open()) throw std::logic_error("WAV writer is already open");

    config.validate();

    if (config.bitsPerSample != 16)
        throw std::invalid_argument("WavFileWriter currently supports only 16-bit PCM");
    if (outputPath.empty()) throw std::invalid_argument("Output path must not be empty");

    config_ = config;
    _output_path = outputPath;
    _blockSize = 0;
    _finalized = false;

    // Open for both reading and writing so the header can be updated.
    const std::string path = outputPath.string();
    _file = std::fopen(path.c_str(), "w+b");

    if (_file == nullptr) throw std::runtime_error("Failed to open WAV output file: " + path);

    try {
        write_header();
    } catch (...) {
        std::fclose(_file);
        _file = nullptr;
        throw;
    }
}

std::size_t wave_file_writer::write(std::span<const std::int16_t> samples) {
    if (!is_open()) throw std::logic_error("WAV output file is not open");
    if (_finalized) throw std::logic_error("Cannot write to a finalized WAV file");

    if (samples.empty()) return 0;

    // Do not allow an incomplete interleaved audio frame.
    if (samples.size() % config_.channelCount != 0)
        throw std::invalid_argument("Sample count must contain complete audio frames");

    constexpr std::uint64_t BYTES_PER_SAMPLE = sizeof(std::int16_t);

    const auto bytesToWrite = static_cast<std::uint64_t>(samples.size()) * BYTES_PER_SAMPLE;

    if (bytesToWrite > MAX_RIFF_DATA_SIZE - _blockSize) {
        throw std::length_error("WAV file exceeds the standard RIFF size limit");
    }

    if (std::fwrite(samples.data(), BYTES_PER_SAMPLE, samples.size(), _file) != samples.size())
        throw std::runtime_error("Failed to write PCM data to WAV file");

    _blockSize += bytesToWrite;
    return samples.size();
}

void wave_file_writer::finalize() {
    if (_finalized) return;
    if (!is_open()) throw std::logic_error("WAV output file is not open");

    update_header();

    if (std::fflush(_file) != 0) throw std::runtime_error("Failed to flush WAV output file");

    const int closeResult = std::fclose(_file);
    _file = nullptr;

    if (closeResult != 0) throw std::runtime_error("Failed to close WAV output file");
    _finalized = true;
}

void wave_file_writer::abort() {
    const bool shouldRemoveOutput = !_finalized;
    if (is_open()) {
        const int closeResult = std::fclose(_file);
        _file = nullptr;
        if (closeResult != 0) throw std::runtime_error("Failed to close WAV output file");
    }
    const bool wasFinalized = _finalized;
    _finalized = true;

    // check the file is present if yes delete it
    std::error_code err;
    if (shouldRemoveOutput && !wasFinalized && !_output_path.empty())
        std::filesystem::remove(_output_path, err);
    _output_path.clear();

    if (err) throw std::system_error(err, "Failed to delete aborted WAV output file");
}

bool wave_file_writer::is_open() const noexcept {
    return _file != nullptr;
}

std::uint64_t wave_file_writer::data_size() const noexcept {
    return _blockSize;
}

void wave_file_writer::write_header() {
    seekTo(_file, 0);

    writeFourCC(_file, "RIFF");
    writeU32LE(_file, RIFF_HEADER_SIZE);
    writeFourCC(_file, "WAVE");

    writeFourCC(_file, "fmt ");
    writeU32LE(_file, 16);  // PCM format chunk size.
    writeU16LE(_file, 1);   // Audio format 1 = integer PCM.
    writeU16LE(_file, config_.channelCount);
    writeU32LE(_file, config_.sampleRate);
    writeU32LE(_file, config_.byteRate());
    writeU16LE(_file, config_.block_align());
    writeU16LE(_file, config_.bitsPerSample);

    writeFourCC(_file, "data");
    writeU32LE(_file, 0);

    seekTo(_file, WAV_HEADER_SIZE);
}

void wave_file_writer::update_header() {
    if (_blockSize > MAX_RIFF_DATA_SIZE)
        throw std::length_error("WAV data exceeds the RIFF size limit");

    seekTo(_file, 4);
    writeU32LE(_file, RIFF_HEADER_SIZE + static_cast<std::uint32_t>(_blockSize));

    seekTo(_file, 40);
    writeU32LE(_file, static_cast<std::uint32_t>(_blockSize));
}
