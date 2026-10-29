#pragma once

#include "recorder_config.h"
#include "spsc_ring_buffer.h"

#include <android/log.h>
#include <oboe/Oboe.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <concepts>
#include <cstdint>
#include <exception>
#include <filesystem>
#include <limits>
#include <memory>
#include <mutex>
#include <span>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

#define AUDIO_RECORDER_LOG_TAG "AUDIO_RECORDER"
#define AUDIO_RECORDER_D(...) \
    __android_log_print(ANDROID_LOG_DEBUG, AUDIO_RECORDER_LOG_TAG, __VA_ARGS__)
#define AUDIO_RECORDER_E(...) \
    __android_log_print(ANDROID_LOG_ERROR, AUDIO_RECORDER_LOG_TAG, __VA_ARGS__)

// The contract every format-specific writer must satisfy so it can be
// driven by audio_recorder.
template <typename Writer>
concept audio_file_writer =
    requires(Writer writer, const std::filesystem::path& outputPath, const recorder_config& config,
             std::span<const std::int16_t> samples) {
        { writer.open(outputPath, config) };
        { writer.write(samples) } -> std::same_as<std::size_t>;
        { writer.finalize() };
        { writer.is_open() } -> std::same_as<bool>;
    };

// Lifecycle and capture pipeline shared by every recording format:
// Oboe input stream -> SPSC ring buffer -> worker thread -> Writer.
// The format itself lives entirely in the Writer type.
template <audio_file_writer Writer>
class audio_recorder final : public oboe::AudioStreamDataCallback, oboe::AudioStreamErrorCallback {
   public:
    audio_recorder() = default;
    ~audio_recorder() override;

    audio_recorder(const audio_recorder&) = delete;
    audio_recorder& operator=(const audio_recorder&) = delete;

    audio_recorder(audio_recorder&&) = delete;
    audio_recorder& operator=(audio_recorder&&) = delete;

    void setOutputFile(const std::filesystem::path& outputPath);
    void setConfig(const recorder_config& config);

    void prepare();
    void start();
    void stop();
    void release();
    void pause();
    void resume();
    void reset();

    [[nodiscard]] bool is_recording() const noexcept;

    [[nodiscard]]
    std::uint64_t dropped_frames() const noexcept;

    [[nodiscard]]
    std::uint32_t sample_rate() const noexcept;

    [[nodiscard]]
    std::uint16_t channel_count() const noexcept;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          std::int32_t numFrames) override;

    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

   private:
    static constexpr std::uint32_t BUFFER_DURATION_SECONDS = 2;
    static constexpr std::size_t WORKER_CHUNK_FRAMES = 1024;

    void start_worker_th();
    void stop_worker_th();
    void worker_loop() noexcept;
    void rethrow_worker_error();

    mutable std::mutex _lifecycle_lock;
    std::mutex _error_lock;
    mutable std::mutex _listener_lock;

    recorder_config _config{};
    std::filesystem::path _output_path;

    std::shared_ptr<oboe::AudioStream> _oboe_stream;
    std::unique_ptr<spsc_ring_buffer<std::int16_t>> _ring_buffer;
    std::unique_ptr<Writer> _writer;
    std::vector<std::int16_t> _worker_buffer;
    std::thread _worker_th;

    std::atomic<bool> _is_recording{false};
    std::atomic<bool> _is_paused{false};
    std::atomic<bool> _is_accepting_audio{false};
    std::atomic<bool> _is_worker_running{false};
    std::atomic<bool> _has_worker_failed{false};
    std::atomic<std::uint64_t> _dropped_frames{0};

    std::exception_ptr _worker_error;

    bool _is_prepared = false;
    bool _is_finalized = false;
};

template <audio_file_writer Writer>
audio_recorder<Writer>::~audio_recorder() {
    try {
        release();
    } catch (...) {
        // Destructors must not throw.
    }
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::setOutputFile(const std::filesystem::path& outputPath) {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Cannot change output file after prepare");

    if (outputPath.empty()) throw std::invalid_argument("Output file path must not be empty");

    _output_path = outputPath;
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::setConfig(const recorder_config& config) {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Cannot change configuration after prepare");

    config.validate();
    _config = config;
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::prepare() {
    std::scoped_lock lock(_lifecycle_lock);

    if (_is_prepared) throw std::logic_error("Recorder is already prepared");

    if (_output_path.empty()) throw std::logic_error("Output file has not been configured");

    _config.validate();

    oboe::AudioStreamBuilder builder;

    auto sampleRate = static_cast<std::int32_t>(_config.sampleRate);

    builder.setDirection(oboe::Direction::Input);
    builder.setFormat(oboe::AudioFormat::I16);
    builder.setChannelCount(_config.channelCount);
    builder.setSampleRate(sampleRate);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setDataCallback(this);
    builder.setErrorCallback(this);

    AUDIO_RECORDER_D(
        "Opening Oboe stream -> Channels: %d, SampleRate: %d, Format: I16, Mode: LowLatency",
        _config.channelCount,
        sampleRate);

    const auto result = builder.openStream(_oboe_stream);

    if (result != oboe::Result::OK || !_oboe_stream) {
        _oboe_stream.reset();
        auto error = std::string(oboe::convertToText(result));
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

        _writer = std::make_unique<Writer>();
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

template <audio_file_writer Writer>
void audio_recorder<Writer>::start() {
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

        auto error = std::string(oboe::convertToText(result));
        throw std::runtime_error("Failed to start Oboe input stream: " + error);
    }

    _is_recording.store(true, std::memory_order_release);
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::stop() {
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

template <audio_file_writer Writer>
void audio_recorder<Writer>::release() {
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

template <audio_file_writer Writer>
bool audio_recorder<Writer>::is_recording() const noexcept {
    return _is_recording.load(std::memory_order_acquire);
}

template <audio_file_writer Writer>
std::uint64_t audio_recorder<Writer>::dropped_frames() const noexcept {
    return _dropped_frames.load(std::memory_order_relaxed);
}

template <audio_file_writer Writer>
std::uint32_t audio_recorder<Writer>::sample_rate() const noexcept {
    return _config.sampleRate;
}

template <audio_file_writer Writer>
std::uint16_t audio_recorder<Writer>::channel_count() const noexcept {
    return _config.channelCount;
}

template <audio_file_writer Writer>
oboe::DataCallbackResult audio_recorder<Writer>::onAudioReady(oboe::AudioStream* /*stream*/,
                                                              void* audioData,
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

    const auto available = _ring_buffer->available_to_write();
    const auto writableSamples = available - (available % channels);
    const auto samplesToWrite = std::min(sampleCount, writableSamples);
    const auto alignedSamples = samplesToWrite - (samplesToWrite % channels);

    std::size_t writtenSamples = 0;

    if (alignedSamples > 0)
        writtenSamples =
            _ring_buffer->write(std::span<const std::int16_t>(samples, alignedSamples));

    // Account for frames actually accepted by the ring buffer.
    const auto acceptedFrames = writtenSamples / channels;
    const auto lostFrames = frameCount - acceptedFrames;

    if (lostFrames > 0) _dropped_frames.fetch_add(lostFrames, std::memory_order_relaxed);
    return oboe::DataCallbackResult::Continue;
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::onErrorAfterClose(oboe::AudioStream*, oboe::Result error) {
    const char* error_message = oboe::convertToText(error);

    AUDIO_RECORDER_E("Oboe stream encountered an error and was closed! Error code: %d (%s)",
                     static_cast<int>(error),
                     error_message);

    // Oboe invokes this after a stream error and closure.
    // A higher-level state/error reporting API can be added later.
    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);

    // connection disconnected
    if (error == oboe::Result::ErrorDisconnected) {
        AUDIO_RECORDER_E("The audio device was disconnected");
    }
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::start_worker_th() {
    if (_worker_th.joinable()) throw std::logic_error("Worker thread is already running");
    _is_worker_running.store(true, std::memory_order_release);

    try {
        _worker_th = std::thread(&audio_recorder<Writer>::worker_loop, this);
    } catch (...) {
        _is_worker_running.store(false, std::memory_order_release);
        throw;
    }
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::stop_worker_th() {
    _is_worker_running.store(false, std::memory_order_release);
    if (_worker_th.joinable()) _worker_th.join();
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::worker_loop() noexcept {
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

            if (alignedAvailable == 0) {
                if (!_is_worker_running.load(std::memory_order_acquire)) break;
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
                continue;
            }

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

template <audio_file_writer Writer>
void audio_recorder<Writer>::rethrow_worker_error() {
    std::exception_ptr error;

    {
        std::scoped_lock lock(_error_lock);
        error = _worker_error;
    }

    if (error) std::rethrow_exception(error);
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::pause() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared || !_oboe_stream) throw std::logic_error("Recorder is not prepared");
    if (!_is_recording.load(std::memory_order_acquire))
        throw std::logic_error("Recorder is not recording");

    _is_accepting_audio.store(false, std::memory_order_release);
    _is_recording.store(false, std::memory_order_release);
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::resume() {
    std::scoped_lock lock(_lifecycle_lock);

    if (!_is_prepared || !_oboe_stream) throw std::logic_error("Recorder is not prepared");

    if (_is_finalized) throw std::logic_error("Cannot resume a finalized recording");

    if (_is_recording.load(std::memory_order_acquire))
        throw std::logic_error("Recorder is already recording");

    rethrow_worker_error();

    _is_accepting_audio.store(true, std::memory_order_release);
    _is_recording.store(true, std::memory_order_release);
}

template <audio_file_writer Writer>
void audio_recorder<Writer>::reset() {
    // release() stops the stream, drains buffered audio, and finalizes
    // the file. It also releases the recorder's native resources.
    release();

    std::scoped_lock lock(_lifecycle_lock);
    _output_path.clear();
    _config = recorder_config{};
    _is_finalized = false;

    _dropped_frames.store(0, std::memory_order_relaxed);
}
