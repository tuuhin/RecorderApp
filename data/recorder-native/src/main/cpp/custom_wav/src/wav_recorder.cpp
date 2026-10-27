#include <algorithm>
#include <chrono>
#include <limits>
#include <stdexcept>
#include <string>

#include "recorder_config.h"
#include "wav_recorder.h"

namespace {

constexpr std::uint32_t BUFFER_DURATION_SECONDS = 2;
constexpr std::size_t WORKER_CHUNK_FRAMES = 1024;

}  // namespace

wav_recorder::~wav_recorder() {
    try {
        release();
    } catch (...) {
        // Destructors must not throw.
    }
}

void wav_recorder::setOutputFile(const std::filesystem::path& outputPath) {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Cannot change output file after prepare");

    if (outputPath.empty()) throw std::invalid_argument("Output file path must not be empty");

    _output_path = outputPath;
}

void wav_recorder::setConfig(const recorder_config& config) {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Cannot change configuration after prepare");

    config.validate();
    _config = config;
}

void wav_recorder::prepare() {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Recorder is already prepared");

    if (_output_path.empty()) throw std::logic_error("Output file has not been configured");

    _config.validate();

    oboe::AudioStreamBuilder builder;

    builder.setDirection(oboe::Direction::Input);
    builder.setFormat(oboe::AudioFormat::I16);
    builder.setChannelCount(_config.channelCount);
    builder.setSampleRate(static_cast<std::int32_t>(_config.sampleRate));
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setDataCallback(this);
    builder.setErrorCallback(this);

    const auto result = builder.openStream(_oboe_stream);

    if (result != oboe::Result::OK || !_oboe_stream) {
        _oboe_stream.reset();
        std::string error = std::string(oboe::convertToText(result));
        throw std::runtime_error("Failed to open Oboe input stream: " + error);
    }

    try {
        if (_oboe_stream->getFormat() != oboe::AudioFormat::I16)
            throw std::runtime_error("Oboe did not open an I16 input stream");

        const auto actualSampleRate = _oboe_stream->getSampleRate();
        const auto actualChannelCount = _oboe_stream->getChannelCount();

        if (actualSampleRate <= 0 || actualChannelCount <= 0)
            throw std::runtime_error("Oboe returned an invalid audio configuration");

        _config.sampleRate = static_cast<std::uint32_t>(actualSampleRate);
        _config.channelCount = static_cast<std::uint16_t>(actualChannelCount);

        _config.validate();

        const auto channelCount = static_cast<std::size_t>(_config.channelCount);
        const auto framesPerSecond = static_cast<std::size_t>(_config.sampleRate);

        if (framesPerSecond >
            std::numeric_limits<std::size_t>::max() / BUFFER_DURATION_SECONDS / channelCount)
            throw std::length_error("Audio buffer capacity exceeds supported range");

        const auto capacity = framesPerSecond * BUFFER_DURATION_SECONDS * channelCount;

        _ring_buffer = std::make_unique<spsc_ring_buffer<std::int16_t>>(capacity);

        _worker_buffer.resize(WORKER_CHUNK_FRAMES * channelCount);

        _writer = std::make_unique<wave_file_writer>();
        _writer->open(_output_path, _config);

        _dropped_frames.store(0, std::memory_order_relaxed);
        _has_worker_failed.store(false, std::memory_order_relaxed);

        {
            std::scoped_lock errorLock(_error_lock);
            _worker_error = nullptr;
        }

        _is_finalized = false;
        _is_prepared = true;
    } catch (...) {
        if (_writer && _writer->is_open()) {
            try {
                _writer->finalize();
            } catch (...) {
            }
        }
        _writer.reset();
        _ring_buffer.reset();
        _worker_buffer.clear();

        _oboe_stream->close();
        _oboe_stream.reset();
        throw;
    }
}

void wav_recorder::start() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared) throw std::logic_error("Recorder must be prepared before start");
    if (_is_recording.load(std::memory_order_acquire))
        throw std::logic_error("Recorder is already recording");
    if (_is_finalized)
        throw std::logic_error("Recorder has been finalized; prepare it again before restarting");

    rethrow_worker_error();

    if (!_oboe_stream || !_ring_buffer || !_writer)
        throw std::logic_error("Recorder resources are unavailable");

    start_worker_th();

    _is_accepting_audio.store(true, std::memory_order_release);

    const auto result = _oboe_stream->requestStart();

    if (result != oboe::Result::OK) {
        _is_accepting_audio.store(false, std::memory_order_release);
        _oboe_stream->requestStop();
        stop_worker_th();

        std::string error = std::string(oboe::convertToText(result));
        throw std::runtime_error("Failed to start Oboe input stream: " + error);
    }

    _is_recording.store(true, std::memory_order_release);
}

void wav_recorder::stop() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared) return;

    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);

    if (_oboe_stream) {
        const auto result = _oboe_stream->requestStop();

        if (result != oboe::Result::OK && result != oboe::Result::ErrorInvalidState) {
            // Continue cleanup even if stopping reports an error.
        }

        // Close the stream before draining the ring buffer.
        // The callback must no longer be running before its resources
        // can be destroyed or the consumer can assume no more samples
        // will arrive.
        _oboe_stream->close();
        _oboe_stream.reset();
    }

    stop_worker_th();

    if (_writer && _writer->is_open()) {
        _writer->finalize();
        _is_finalized = true;
    }

    rethrow_worker_error();
}

void wav_recorder::release() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared && !_oboe_stream && !_worker_th.joinable()) return;

    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);

    if (_oboe_stream) {
        _oboe_stream->requestStop();
        _oboe_stream->close();
        _oboe_stream.reset();
    }

    stop_worker_th();

    std::exception_ptr finalizationError;

    if (_writer && _writer->is_open()) {
        try {
            _writer->finalize();
            _is_finalized = true;
        } catch (...) {
            finalizationError = std::current_exception();
        }
    }

    _writer.reset();
    _ring_buffer.reset();
    _worker_buffer.clear();

    _is_prepared = false;

    if (finalizationError) std::rethrow_exception(finalizationError);

    rethrow_worker_error();
}

bool wav_recorder::is_recording() const noexcept {
    return _is_recording.load(std::memory_order_acquire);
}

std::uint64_t wav_recorder::dropped_frames() const noexcept {
    return _dropped_frames.load(std::memory_order_relaxed);
}

std::uint32_t wav_recorder::sample_rate() const noexcept {
    return _config.sampleRate;
}

std::uint16_t wav_recorder::channel_count() const noexcept {
    return _config.channelCount;
}

oboe::DataCallbackResult wav_recorder::onAudioReady(oboe::AudioStream* /*stream*/, void* audioData,
                                                    std::int32_t numFrames) {
    if (!_is_accepting_audio.load(std::memory_order_acquire) ||
        _has_worker_failed.load(std::memory_order_acquire))
        return oboe::DataCallbackResult::Stop;

    if (audioData == nullptr || numFrames <= 0 || !_ring_buffer)
        return oboe::DataCallbackResult::Continue;

    const auto channels = static_cast<std::size_t>(_config.channelCount);

    const auto frameCount = static_cast<std::size_t>(numFrames);
    const auto sampleCount = frameCount * channels;

    const auto* samples = static_cast<const std::int16_t*>(audioData);

    // Preserve complete interleaved frames. Never write only half
    // of a stereo frame if the ring buffer has insufficient space.
    const auto available = _ring_buffer->available_to_write();

    const auto writableSamples = available - (available % channels);
    const auto samplesToWrite = std::min(sampleCount, writableSamples);
    const auto alignedSamples = samplesToWrite - (samplesToWrite % channels);

    if (alignedSamples > 0) {
        _ring_buffer->write(std::span<const std::int16_t>(samples, alignedSamples));
    }

    const auto acceptedFrames = alignedSamples / channels;
    const auto lostFrames = frameCount - acceptedFrames;

    if (lostFrames > 0) {
        _dropped_frames.fetch_add(lostFrames, std::memory_order_relaxed);
    }

    return oboe::DataCallbackResult::Continue;
}

void wav_recorder::onErrorAfterClose(oboe::AudioStream*, oboe::Result) {
    // Oboe invokes this after a stream error and closure.
    // A higher-level state/error reporting API can be added later.
    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);
}

void wav_recorder::start_worker_th() {
    if (_worker_th.joinable()) throw std::logic_error("Worker thread is already running");
    _is_worker_running.store(true, std::memory_order_release);

    try {
        _worker_th = std::thread(&wav_recorder::worker_loop, this);
    } catch (...) {
        _is_worker_running.store(false, std::memory_order_release);
        throw;
    }
}

void wav_recorder::stop_worker_th() {
    _is_worker_running.store(false, std::memory_order_release);
    if (_worker_th.joinable()) _worker_th.join();
}

void wav_recorder::worker_loop() noexcept {
    try {
        while (true) {
            const auto available = _ring_buffer->available_to_read();

            if (available == 0) {
                if (!_is_worker_running.load(std::memory_order_acquire)) break;

                std::this_thread::sleep_for(std::chrono::milliseconds(1));
                continue;
            }

            const auto channels = static_cast<std::size_t>(_config.channelCount);
            const auto alignedAvailable = available - (available % channels);

            if (alignedAvailable == 0) continue;

            const auto count = _ring_buffer->read(std::span<std::int16_t>(
                _worker_buffer.data(), std::min(alignedAvailable, _worker_buffer.size())));

            if (count > 0)
                _writer->write(std::span<const std::int16_t>(_worker_buffer.data(), count));
        }
    } catch (...) {
        {
            std::scoped_lock lock(_error_lock);
            _worker_error = std::current_exception();
        }

        _has_worker_failed.store(true, std::memory_order_release);
        _is_accepting_audio.store(false, std::memory_order_release);
    }
}

void wav_recorder::rethrow_worker_error() {
    std::exception_ptr error;

    {
        std::scoped_lock lock(_error_lock);
        error = _worker_error;
    }

    if (error) std::rethrow_exception(error);
}

void wav_recorder::pause() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared || !_oboe_stream) throw std::logic_error("Recorder is not prepared");
    if (!_is_recording.load(std::memory_order_acquire))
        throw std::logic_error("Recorder is not recording");

    const auto result = _oboe_stream->requestPause();

    if (result != oboe::Result::OK) {
        std::string error = std::string(oboe::convertToText(result));
        throw std::runtime_error("Failed to start Oboe input stream: " + error);
    }

    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);
}

void wav_recorder::resume() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared || !_oboe_stream) throw std::logic_error("Recorder is not prepared");

    if (_is_finalized) throw std::logic_error("Cannot resume a finalized recording");

    if (_is_recording.load(std::memory_order_acquire))
        throw std::logic_error("Recorder is already recording");

    _is_accepting_audio.store(true, std::memory_order_release);

    const auto result = _oboe_stream->requestStart();

    if (result != oboe::Result::OK) {
        _is_accepting_audio.store(false, std::memory_order_release);
        std::string error = std::string(oboe::convertToText(result));
        throw std::runtime_error("Failed to start Oboe input stream: " + error);
    }

    _is_recording.store(true, std::memory_order_release);
}

void wav_recorder::reset() {
    // release() stops the stream, drains buffered audio, and finalizes
    // the file. It also releases the recorder's native resources.
    release();

    std::scoped_lock lock(_lifecycle_lock);
    _output_path.clear();
    _config = recorder_config{};
    _is_finalized = false;

    _dropped_frames.store(0, std::memory_order_relaxed);
}