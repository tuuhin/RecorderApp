#include <algorithm>
#include <limits>
#include <stdexcept>
#include <string>

#include "flac_file_writer.h"

namespace {

// realtime microphone recording on mobile hardware.
constexpr std::uint32_t FLAC_COMPRESSION_LEVEL = 5;

}  // namespace

flac_file_writer::~flac_file_writer() noexcept {
    if (is_open()) {
        try {
            finalize();
        } catch (...) {
        }
    }

    // finalize() rethrows a failed finish() before deleting; release
    // the encoder here so a failed finalization cannot leak it.
    if (_encoder != nullptr) {
        FLAC__stream_encoder_delete(_encoder);
        _encoder = nullptr;
    }
}

void flac_file_writer::open(const std::filesystem::path& outputPath,
                            const recorder_config& config) {
    if (is_open()) throw std::logic_error("FLAC writer is already open");

    config.validate();

    if (config.bitsPerSample != 16)
        throw std::invalid_argument("FlacFileWriter currently supports only 16-bit PCM");
    if (outputPath.empty()) throw std::invalid_argument("Output path must not be empty");

    config_ = config;
    _frames_written = 0;
    _finalized = false;

    _encoder = FLAC__stream_encoder_new();

    if (_encoder == nullptr) throw std::runtime_error("Failed to allocate FLAC encoder");

    try {
        if (!FLAC__stream_encoder_set_channels(_encoder, config_.channelCount))
            throw std::runtime_error("Failed to configure FLAC encoder channels");

        if (!FLAC__stream_encoder_set_bits_per_sample(_encoder, config_.bitsPerSample))
            throw std::runtime_error("Failed to configure FLAC encoder sample width");

        if (!FLAC__stream_encoder_set_sample_rate(_encoder, config_.sampleRate))
            throw std::runtime_error("Failed to configure FLAC encoder sample rate");

        if (!FLAC__stream_encoder_set_compression_level(_encoder, FLAC_COMPRESSION_LEVEL))
            throw std::runtime_error("Failed to configure FLAC encoder compression level");

        // The file is opened for update, so finish() can seek back and
        // rewrite the STREAMINFO block with the final stream statistics.
        const auto status =
            FLAC__stream_encoder_init_file(_encoder, outputPath.string().c_str(), nullptr, nullptr);

        if (status != FLAC__STREAM_ENCODER_INIT_STATUS_OK) {
            throw std::runtime_error("Failed to open FLAC output file '" + outputPath.string() +
                                     "': " + FLAC__StreamEncoderInitStatusString[status]);
        }
    } catch (...) {
        FLAC__stream_encoder_delete(_encoder);
        _encoder = nullptr;
        throw;
    }
}

std::size_t flac_file_writer::write(std::span<const std::int16_t> samples) {
    if (!is_open()) throw std::logic_error("FLAC encoder is not open");
    if (_finalized) throw std::logic_error("Cannot write to a finalized FLAC file");

    if (samples.empty()) return 0;

    if (samples.size() % config_.channelCount != 0)
        throw std::invalid_argument("Sample count must contain complete audio frames");

    const auto frames = samples.size() / config_.channelCount;

    if (frames > std::numeric_limits<FLAC__uint32>::max())
        throw std::length_error("Write exceeds the supported FLAC frame count");

    _sample_buffer.assign(samples.begin(), samples.end());

    if (!FLAC__stream_encoder_process_interleaved(
            _encoder, _sample_buffer.data(), static_cast<FLAC__uint32>(frames)))
        throw std::runtime_error(std::string("Failed to encode FLAC frames: ") +
                                 FLAC__stream_encoder_get_resolved_state_string(_encoder));

    _frames_written += frames;
    return samples.size();
}

void flac_file_writer::finalize() {
    if (!is_open()) throw std::logic_error("FLAC encoder is not open");

    if (_finalized) return;

    // Encodes any buffered input, flushes, and rewrites the STREAMINFO
    // block with the final statistics of the stream.
    if (!FLAC__stream_encoder_finish(_encoder))
        throw std::runtime_error(std::string("Failed to finalize FLAC stream: ") +
                                 FLAC__stream_encoder_get_resolved_state_string(_encoder));

    FLAC__stream_encoder_delete(_encoder);
    _encoder = nullptr;
    _finalized = true;
}

bool flac_file_writer::is_open() const noexcept {
    return _encoder != nullptr;
}

std::uint64_t flac_file_writer::frames_written() const noexcept {
    return _frames_written;
}
