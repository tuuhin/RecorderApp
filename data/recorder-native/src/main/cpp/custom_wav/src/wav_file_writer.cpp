#include <array>
#include <bit>
#include <cstddef>
#include <limits>
#include <stdexcept>
#include <string>
#include <vector>

#include "recorder_config.h"
#include "wav_file_writer.h"

namespace {

constexpr std::uint32_t RIFF_HEADER_SIZE = 36;
constexpr std::uint32_t WAV_HEADER_SIZE = 44;

// RIFF stores its chunk size in a 32-bit unsigned field.
constexpr std::uint64_t MAX_RIFF_DATA_SIZE =
    std::numeric_limits<std::uint32_t>::max() - RIFF_HEADER_SIZE;

void writeU16LE(std::ostream& stream, std::uint16_t value) {
    const std::array<char, 2> bytes{static_cast<char>(value & 0xFF),
                                    static_cast<char>((value >> 8) & 0xFF)};

    stream.write(bytes.data(), static_cast<std::streamsize>(bytes.size()));
}

void writeU32LE(std::ostream& stream, std::uint32_t value) {
    const std::array<char, 4> bytes{static_cast<char>(value & 0xFF),
                                    static_cast<char>((value >> 8) & 0xFF),
                                    static_cast<char>((value >> 16) & 0xFF),
                                    static_cast<char>((value >> 24) & 0xFF)};

    stream.write(bytes.data(), static_cast<std::streamsize>(bytes.size()));
}

void writeFourCC(std::ostream& stream, const char (&id)[5]) {
    stream.write(id, 4);
}

}  // namespace

wave_file_writer::~wave_file_writer() noexcept {
    if (is_open() && !_finalized) {
        try {
            finalize();
        } catch (...) {
            // Destructors must not throw.
            // Call finalize() explicitly to handle errors.
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
    _blockSize = 0;
    _finalized = false;

    // Open for both reading and writing so the header can be updated.
    _file.open(outputPath, std::ios::binary | std::ios::in | std::ios::out | std::ios::trunc);

    if (!_file.is_open())
        throw std::runtime_error("Failed to open WAV output file: " + outputPath.string());

    try {
        write_header();
    } catch (...) {
        _file.close();
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

    _file.write(reinterpret_cast<const char*>(samples.data()),
                static_cast<std::streamsize>(bytesToWrite));

    if (!_file) throw std::runtime_error("Failed to write PCM data to WAV file");

    _blockSize += bytesToWrite;
    return samples.size();
}

void wave_file_writer::finalize() {
    if (!is_open()) throw std::logic_error("WAV output file is not open");

    if (_finalized) return;

    update_header();
    _file.flush();

    if (!_file) throw std::runtime_error("Failed to flush WAV output file");
    _file.close();

    if (_file.fail()) throw std::runtime_error("Failed to close WAV output file");
    _finalized = true;
}

bool wave_file_writer::is_open() const noexcept {
    return _file.is_open();
}

std::uint64_t wave_file_writer::data_size() const noexcept {
    return _blockSize;
}

void wave_file_writer::write_header() {
    _file.seekp(0, std::ios::beg);

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

    if (!_file) throw std::runtime_error("Failed to write WAV header");

    _file.seekp(WAV_HEADER_SIZE, std::ios::beg);

    if (!_file) throw std::runtime_error("Failed to position WAV data stream");
}

void wave_file_writer::update_header() {
    if (_blockSize > MAX_RIFF_DATA_SIZE)
        throw std::length_error("WAV data exceeds the RIFF size limit");

    _file.seekp(4, std::ios::beg);
    writeU32LE(_file, RIFF_HEADER_SIZE + static_cast<std::uint32_t>(_blockSize));

    _file.seekp(40, std::ios::beg);
    writeU32LE(_file, static_cast<std::uint32_t>(_blockSize));

    if (!_file) throw std::runtime_error("Failed to finalize WAV header");
}
