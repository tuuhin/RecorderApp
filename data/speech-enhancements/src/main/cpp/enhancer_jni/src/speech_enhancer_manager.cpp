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

// SNR-dependent blend thresholds.
constexpr float kAdaptiveFullDb = 5.0f;
constexpr float kAdaptiveFloorDb = 20.0f;
constexpr float kAdaptiveFloorStrength = 0.15f;

// Gate so quiet noise is not treated as clean speech.
constexpr float kAdaptiveSpeechGateDb = 15.0f;

// Speech reference release rate (dB per frame).
constexpr float kReferenceReleaseDbPerFrame = 0.05f;

[[nodiscard]] float blend_for_lsnr(float lsnr, float max_strength) noexcept {
    const float t =
        std::clamp((lsnr - kAdaptiveFullDb) / (kAdaptiveFloorDb - kAdaptiveFullDb), 0.0f, 1.0f);
    const float floor = std::min(kAdaptiveFloorStrength, max_strength);
    return std::lerp(max_strength, floor, t);
}

[[nodiscard]] bool file_exists(const std::filesystem::path& path) noexcept {
    std::error_code ec;
    return std::filesystem::exists(path, ec);
}

[[nodiscard]] std::filesystem::path join_path(std::string_view directory, std::string_view name) {
    return std::filesystem::path{directory} / name;
}

}  // namespace

speech_enhancer_manager::speech_enhancer_manager(std::string model_dir)
    : _model_dir{[&model_dir]() {
          if (model_dir.empty()) throw std::invalid_argument("Model directory must not be empty");
          std::error_code ec;
          const auto canonical = std::filesystem::weakly_canonical(model_dir, ec);
          return ec ? model_dir : canonical.string();
      }()} {}

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
    _stream = stream_state{};
    if (!_model_session) return false;

    const bool has_consumed_state = _model_session->stats().input_samples > 0;
    if (has_consumed_state) _model_session->reset();

    return has_consumed_state;
}

int speech_enhancer_manager::latency_samples() {
    const std::scoped_lock lock(mutex_);
    return config().latency_samples();
}

std::span<const float> speech_enhancer_manager::stream_process(std::span<const float> samples,
                                                               const enhance_options& options) {
    const std::scoped_lock lock(mutex_);

    if (_stream.finished)
        throw std::logic_error("the stream is already finished; call reset() to start a new one");
    if (samples.empty()) return {};

    ensure_engine();

    // Model-side stages, applied per call.
    _model_session->engine().set_post_filter_beta(options.post_filter_beta);
    _model_session->engine().set_attenuation_limit_db(options.attenuation_limit_db);

    // Keep the input that the freshly emitted output lines up with.
    _stream.input_history.insert(_stream.input_history.end(), samples.begin(), samples.end());
    _stream.consumed += samples.size();

    const std::uint64_t emitted_before = _stream.emitted;
    std::vector<float> raw;
    raw.reserve(samples.size() + static_cast<std::size_t>(_model_session->latency_samples()) + 1);
    _model_session->process(samples, raw);
    _stream.emitted += raw.size();

    blend_emitted(raw, emitted_before, options);
    trim_history();

    return _results;
}

std::span<const float> speech_enhancer_manager::stream_finish(const enhance_options& options) {
    const std::scoped_lock lock(mutex_);

    if (_stream.finished)
        throw std::logic_error("the stream is already finished; call reset() to start a new one");

    ensure_engine();

    _model_session->engine().set_post_filter_beta(options.post_filter_beta);
    _model_session->engine().set_attenuation_limit_db(options.attenuation_limit_db);

    const std::uint64_t emitted_before = _stream.emitted;
    std::vector<float> raw;
    _model_session->flush(raw);
    _stream.emitted += raw.size();

    blend_emitted(raw, emitted_before, options);
    trim_history();

    _stream.finished = true;
    return _results;
}

void speech_enhancer_manager::blend_emitted(std::vector<float>& batch,
                                            const std::uint64_t emitted_before,
                                            const enhance_options& options) {
    const auto latency = static_cast<std::uint64_t>(_model_session->latency_samples());
    const auto hop = static_cast<std::uint64_t>(_model_session->config().hop_size);
    const auto lookahead_hops = latency / hop;
    const auto consumed = _stream.consumed;
    const std::span<const float> lsnrs{_model_session->frame_lsnrs()};
    const float max_strength = std::clamp(options.strength, 0.0f, 1.0f);

    // Emitted sample j matches input sample j - latency.
    _results.assign(batch.begin(), batch.end());

    const bool blend_strength = max_strength < 1.0f;
    const bool blend_adaptive = options.adaptive_strength;
    if (!blend_strength && !blend_adaptive) return;

    for (const std::weakly_incrementable auto e :
         std::views::iota(emitted_before / hop, _stream.emitted / hop)) {
        float alpha = max_strength;

        if (blend_adaptive && e >= lookahead_hops) {
            const std::uint64_t frame = e - lookahead_hops;

            // Frame energy of the matching input hop.
            double energy = 0.0;
            std::size_t count = 0;
            for (const std::uint64_t i :
                 std::views::iota(frame * hop, std::min((frame + 1) * hop, consumed))) {
                const float sample = input_at(i);
                energy += static_cast<double>(sample) * static_cast<double>(sample);
                ++count;
            }
            const auto rms = static_cast<float>(
                std::sqrt(energy / static_cast<double>(std::max(count, std::size_t{1}))));
            const float frame_db = 20.0f * std::log10(rms + 1e-12f);

            // Running speech reference: fast attack, slow release.
            _stream.speech_reference_db =
                std::max(frame_db, _stream.speech_reference_db - kReferenceReleaseDbPerFrame);

            const bool speech_like =
                frame_db >= _stream.speech_reference_db - kAdaptiveSpeechGateDb;
            const float lsnr = frame < lsnrs.size() ? lsnrs[frame] : kAdaptiveFullDb;
            if (speech_like) alpha = blend_for_lsnr(lsnr, max_strength);
        }

        if (!blend_adaptive && !blend_strength) continue;

        for (const std::weakly_incrementable auto k : std::views::iota(std::uint64_t{0}, hop)) {
            const std::uint64_t stream_index = e * hop + k;
            const std::uint64_t input_index = stream_index - latency;
            if (input_index >= consumed) continue;  // padded tail: keep the model output
            const auto offset = static_cast<std::size_t>(stream_index - emitted_before);
            _results[offset] = std::lerp(input_at(input_index), _results[offset], alpha);
        }
    }
}

void speech_enhancer_manager::trim_history() {
    const auto latency = static_cast<std::uint64_t>(_model_session->latency_samples());
    const std::uint64_t keep_from = _stream.emitted >= latency ? _stream.emitted - latency : 0;
    while (_stream.history_start < keep_from && !_stream.input_history.empty()) {
        _stream.input_history.pop_front();
        ++_stream.history_start;
    }
}

float speech_enhancer_manager::input_at(const std::uint64_t index) const {
    const auto offset = static_cast<std::size_t>(index - _stream.history_start);
    return _stream.input_history[offset];
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
