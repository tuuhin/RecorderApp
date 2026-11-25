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

// SNR-dependent output blend. The model over-suppresses already-clean speech (measured: it
// costs ~7-10 dB of SNR on clean clips, while the official PyTorch model leaves them
// untouched), so the full model output is only trusted on noisy frames. `lsnr <= kFullDb`
// keeps the full model output, `lsnr >= kFloorDb` fades down to `kFloorStrength`, linear in
// between. Speech frames have a high local SNR and get protected; noise frames have a low
// one and are still fully suppressed. The thresholds were tuned on the Valentini clips so
// every clip gains or loses at most ~1 dB (mean +2.5 dB, dropouts roughly halved).
constexpr float kAdaptiveFullDb = 5.0f;
constexpr float kAdaptiveFloorDb = 20.0f;
constexpr float kAdaptiveFloorStrength = 0.15f;

// A frame is only eligible for the clean-speech protection when it is within this many dB of
// the clip's loudest frames. A quiet noise floor has a high local SNR (little noise relative
// to nothing), so without this gate the adaptive blend treats a hissy background as clean
// speech and blends it back in.
constexpr float kAdaptiveSpeechGateDb = 15.0f;

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
                                                        int sample_rate,
                                                        const enhance_options& options) {
    if (sample_rate != kModelSampleRate)
        throw std::invalid_argument("the native engine processes 48 kHz input only");

    const std::scoped_lock lock(mutex_);
    ensure_engine();

    // Model-side stages; they live on the engine and are re-applied per call.
    _model_session->engine().set_post_filter_beta(options.post_filter_beta);
    _model_session->engine().set_attenuation_limit_db(options.attenuation_limit_db);

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

    // The offline pass can return up to `hop_size - 1` extra samples when the input length
    // is not a whole number of hops (the flush pads the tail to whole hops), and defensively
    // could return fewer. Trim or pad so the result always aligns 1:1 with the input.
    _results.resize(samples.size(), 0.0f);

    const float max_strength = std::clamp(options.strength, 0.0f, 1.0f);

    if (options.adaptive_strength) {
        const auto hop = static_cast<std::size_t>(_model_session->config().hop_size);
        const std::vector<float>& lsnrs = _model_session->frame_lsnrs();
        const std::size_t frame_count = (_results.size() + hop - 1) / hop;

        std::vector<float> frame_db(frame_count, -200.0f);
        for (const std::weakly_incrementable auto f :
             std::views::iota(std::size_t{0}, frame_count)) {
            const std::size_t begin = f * hop;
            const std::size_t end = std::min(begin + hop, samples.size());
            if (end <= begin) continue;
            double energy = 0.0;
            for (std::size_t i = begin; i < end; ++i) {
                energy += static_cast<double>(samples[i]) * static_cast<double>(samples[i]);
            }
            const auto rms =
                static_cast<float>(std::sqrt(energy / static_cast<double>(end - begin)));
            frame_db[f] = 20.0f * std::log10(rms + 1e-12f);
        }
        std::vector<float> sorted_db = frame_db;
        std::sort(sorted_db.begin(), sorted_db.end());
        const float reference_db =
            sorted_db.empty() ? 0.0f : sorted_db[(sorted_db.size() * 9) / 10];
        const float speech_gate_db = reference_db - kAdaptiveSpeechGateDb;

        for (const std::weakly_incrementable auto f :
             std::views::iota(std::size_t{0}, frame_count)) {
            const std::size_t begin = f * hop;
            const std::size_t end = std::min(begin + hop, _results.size());
            const float lsnr = f < lsnrs.size() ? lsnrs[f] : kAdaptiveFullDb;
            const bool speech_like = frame_db[f] >= speech_gate_db;
            const float alpha = speech_like ? blend_for_lsnr(lsnr, max_strength) : max_strength;

            for (const std::weakly_incrementable auto i : std::views::iota(begin, end)) {
                _results[i] = std::lerp(samples[i], _results[i], alpha);
            }
        }
    } else if (max_strength < 1.0f) {
        const std::size_t blend = std::min(_results.size(), samples.size());
        for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, blend)) {
            _results[i] = std::lerp(samples[i], _results[i], max_strength);
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
