#include "speech_enhancer_manager.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <filesystem>
#include <format>
#include <memory>
#include <mutex>
#include <ranges>
#include <span>
#include <stdexcept>
#include <string>
#include <string_view>
#include <system_error>
#include <utility>
#include <vector>

namespace dfn::jni {
namespace {

constexpr std::string_view kConfigFile = "config.ini";
constexpr std::string_view kEncoderGraph = "enc.onnx";
constexpr std::string_view kErbDecoderGraph = "erb_dec.onnx";
constexpr std::string_view kDfDecoderGraph = "df_dec.onnx";

[[nodiscard]] bool file_exists(const std::filesystem::path& path) noexcept {
    std::error_code ec;
    return std::filesystem::exists(path, ec);
}

[[nodiscard]] std::filesystem::path join_path(std::string_view directory, std::string_view name) {
    return std::filesystem::path{directory} / name;
}

}  // namespace

speech_enhancer_manager::speech_enhancer_manager(std::string model_dir)
    : _model_dir(std::move(model_dir)) {}

bool speech_enhancer_manager::prepare() {
    const std::scoped_lock lock(mutex_);

    if (!validate_model_path()) return false;

    std::string error;
    auto loaded =
        std::make_shared<dfn::enhancer::model_config>(dfn::enhancer::model_config::from_ini_file(
            join_path(_model_dir, kConfigFile).string(), &error));

    loaded->validate();
    _model_config = std::move(loaded);
    return true;
}

bool speech_enhancer_manager::reset() {
    const std::scoped_lock lock(mutex_);

    _results.clear();
    if (!_model_session) return false;

    const bool has_consumed_state = _model_session->stats().input_samples > 0;
    if (has_consumed_state) _model_session->reset();

    // if we had consumed state then reset was successful
    return has_consumed_state;
}

int speech_enhancer_manager::latency_samples() {
    const std::scoped_lock lock(mutex_);
    return config().latency_samples();
}

std::span<const float> speech_enhancer_manager::enhance(std::span<const float> samples,
                                                        int sample_rate, float strength) {
    if (sample_rate != kModelSampleRate)
        throw std::invalid_argument("the native engine processes 48 kHz input only");

    const std::scoped_lock lock(mutex_);
    ensure_engine();

    std::vector<float> raw;
    raw.reserve(samples.size() + static_cast<std::size_t>(_model_session->latency_samples()) + 1);
    _model_session->process(samples, raw);
    _model_session->flush(raw);

    // Delay compensation: drop the pipeline's latency from the head so the output lines
    // up with the input. The flush padded the input tail to whole hops, so every input
    // sample is covered.
    const auto latency = static_cast<std::size_t>(_model_session->latency_samples());
    const std::size_t drop = std::min(latency, raw.size());
    _results.assign(raw.begin() + static_cast<std::ptrdiff_t>(drop), raw.end());

    if (_results.size() < samples.size()) _results.resize(samples.size(), 0.0f);

    // Blend the model output with the input; 1.0 is the full model output. Clamped so a
    // stray value degrades into a blend instead of amplifying the input.
    const float alpha = std::clamp(strength, 0.0f, 1.0f);
    if (alpha < 1.0f) {
        const std::size_t blend = std::min(_results.size(), samples.size());
        for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, blend)) {
            _results[i] = std::lerp(samples[i], _results[i], alpha);
        }
    }

    return _results;
}

bool speech_enhancer_manager::validate_model_path() const {
    return file_exists(join_path(_model_dir, kConfigFile)) &&
           file_exists(join_path(_model_dir, kEncoderGraph)) &&
           file_exists(join_path(_model_dir, kErbDecoderGraph)) &&
           file_exists(join_path(_model_dir, kDfDecoderGraph));
}

const dfn::enhancer::model_config& speech_enhancer_manager::config() {
    // Called with mutex_ held.
    if (_model_config) return *_model_config;

    _model_config = std::make_shared<dfn::enhancer::model_config>(
        dfn::enhancer::model_config::from_ini_file(join_path(_model_dir, kConfigFile).string()));
    _model_config->validate();

    return *_model_config;
}

void speech_enhancer_manager::ensure_engine() {
    // Called with mutex_ held.
    if (_model_session) return;

    if (!validate_model_path())
        throw std::runtime_error(std::format("model files are missing at {}", _model_dir));

    std::string error;
    auto backend = dfn::enhancer::onnx_backend_runner::from_files(
        join_path(_model_dir, kEncoderGraph).string(),
        join_path(_model_dir, kErbDecoderGraph).string(),
        join_path(_model_dir, kDfDecoderGraph).string(),
        config(),
        dfn::enhancer::onnx_backend_runner::Options{},
        &error);

    if (!backend) throw std::runtime_error(error.empty() ? "failed to load" : error);

    _model_runner = std::move(backend);
    _model_session = std::make_shared<dfn::enhancer::EnhancerSession>(_model_runner, config());
}

}  // namespace dfn::jni
