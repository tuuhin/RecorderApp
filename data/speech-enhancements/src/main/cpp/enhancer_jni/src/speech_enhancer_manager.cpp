#include "speech_enhancer_manager.h"

#include <algorithm>
#include <cstdio>
#include <filesystem>
#include <memory>
#include <stdexcept>
#include <string>
#include <utility>

namespace dfn::jni {
namespace {

constexpr const char* kConfigFile = "config.ini";
constexpr const char* kEncoderGraph = "enc_stream.onnx";
constexpr const char* kErbDecoderGraph = "erb_dec_stream.onnx";
constexpr const char* kDfDecoderGraph = "df_dec_stream.onnx";

bool file_exists(const std::string& path) {
    std::error_code ec;
    return std::filesystem::exists(path, ec);
}

std::string join_path(const std::string& directory, const char* name) {
    std::string trimmed = directory;
    while (!trimmed.empty() && trimmed.back() == '/') trimmed.pop_back();
    return trimmed.empty() ? std::string(name) : trimmed + "/" + name;
}

}  // namespace

speech_enhancer_manager::speech_enhancer_manager(std::string modelDir)
    : model_dir_(std::move(modelDir)) {}

bool speech_enhancer_manager::prepare() {
    const std::scoped_lock lock(mutex_);
    if (!validate_model_path()) return false;

    std::string error;
    auto config = std::make_shared<dfn::enhancer::ModelConfig>(
        dfn::enhancer::ModelConfig::from_ini_file(join_path(model_dir_, kConfigFile), &error));
    config->validate();
    _model_config = std::move(config);
    return true;
}

int speech_enhancer_manager::latency_samples() {
    const std::scoped_lock lock(mutex_);
    return _config().latency_samples();
}

std::span<const float> speech_enhancer_manager::enhance(std::span<const float> samples,
                                                        int sample_rate, float strength) {
    if (sample_rate != kModelSampleRate)
        throw std::invalid_argument("the native engine processes 48 kHz input only");

    const std::scoped_lock lock(mutex_);
    _ensure_engine();

    std::vector<float> raw;
    raw.reserve(samples.size() + static_cast<std::size_t>(_model_session->latency_samples()) + 1);
    _model_session->process(samples.data(), samples.size(), raw);
    _model_session->flush(raw);

    // Delay compensation: drop the pipeline's latency from the head so the output lines
    // up with the input. The flush padded the input tail to whole hops, so every input
    // sample is covered.
    const auto latency = static_cast<std::size_t>(_model_session->latency_samples());
    const std::size_t drop = std::min(latency, raw.size());
    output_.assign(raw.begin() + static_cast<std::ptrdiff_t>(drop), raw.end());
    if (output_.size() < samples.size()) output_.resize(samples.size(), 0.0f);

    // Blend the model output with the input; 1.0 is the full model output. Clamped so a
    // stray value degrades into a blend instead of amplifying the input.
    const float alpha = std::clamp(strength, 0.0f, 1.0f);
    if (alpha < 1.0f) {
        const std::size_t blend = std::min(output_.size(), samples.size());
        for (std::size_t i = 0; i < blend; ++i) {
            output_[i] = output_[i] * alpha + samples[i] * (1.0f - alpha);
        }
    }
    return output_;
}

bool speech_enhancer_manager::validate_model_path() const {
    return file_exists(join_path(model_dir_, kConfigFile)) &&
           file_exists(join_path(model_dir_, kEncoderGraph)) &&
           file_exists(join_path(model_dir_, kErbDecoderGraph)) &&
           file_exists(join_path(model_dir_, kDfDecoderGraph));
}

const dfn::enhancer::ModelConfig& speech_enhancer_manager::_config() {
    // Called with mutex_ held.
    if (_model_config) return *_model_config;

    _model_config = std::make_shared<dfn::enhancer::ModelConfig>(
        dfn::enhancer::ModelConfig::from_ini_file(join_path(model_dir_, kConfigFile)));
    _model_config->validate();

    return *_model_config;
}

void speech_enhancer_manager::_ensure_engine() {
    // Called with mutex_ held.
    if (_model_session) return;

    // invalid model path
    if (!validate_model_path())
        throw std::runtime_error("model files are missing at " + model_dir_);

    std::string error;
    auto backend = dfn::enhancer::OnnxBackend::from_files(join_path(model_dir_, kEncoderGraph),
                                                          join_path(model_dir_, kErbDecoderGraph),
                                                          join_path(model_dir_, kDfDecoderGraph),
                                                          _config(),
                                                          dfn::enhancer::OnnxBackend::Options{},
                                                          &error);

    // backend failed
    if (!backend) throw std::runtime_error(error.empty() ? "failed to load" : error);

    _model_runner = std::move(backend);
    _model_session = std::make_shared<dfn::enhancer::EnhancerSession>(_model_runner, _config());
}

}  // namespace dfn::jni
