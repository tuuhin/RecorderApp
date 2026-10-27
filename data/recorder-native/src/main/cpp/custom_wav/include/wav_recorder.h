#pragma once

#include "recorder_config.h"
#include "spsc_ring_buffer.h"
#include "wav_file_writer.h"

#include <oboe/Oboe.h>

#include <atomic>
#include <cstdint>
#include <exception>
#include <filesystem>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

class wav_recorder final : public oboe::AudioStreamDataCallback, oboe::AudioStreamErrorCallback {
   public:
    wav_recorder() = default;
    ~wav_recorder() override;

    wav_recorder(const wav_recorder&) = delete;
    wav_recorder& operator=(const wav_recorder&) = delete;

    wav_recorder(wav_recorder&&) = delete;
    wav_recorder& operator=(wav_recorder&&) = delete;

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
    std::unique_ptr<wave_file_writer> _writer;
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
