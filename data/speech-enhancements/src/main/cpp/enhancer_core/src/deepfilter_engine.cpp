#include "deepfilter_engine.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <stdexcept>
#include <utility>

#include "deep_filter.h"
#include "dsp_features.h"
#include "erb.h"

namespace dfn::enhancer {
namespace {

template <typename T>
void shift_and_append(std::vector<T>& window, std::size_t frame_length, const T* frame) {
    if (window.size() > frame_length) {
        std::memmove(window.data(),
                     window.data() + frame_length,
                     sizeof(T) * (window.size() - frame_length));
    }
    std::copy(
        frame, frame + frame_length, window.end() - static_cast<std::ptrdiff_t>(frame_length));
}

}  // namespace

DeepFilterEngine::DeepFilterEngine(ModelConfig config, std::shared_ptr<ModelRunner> runner,
                                   EnhancerOptions options)
    : state_(std::move(config)), runner_(std::move(runner)), options_(options) {
    if (runner_ == nullptr) {
        throw std::invalid_argument("DeepFilterEngine requires a model runner");
    }
    state_.config.validate();
    erb_widths_ = dsp::erb_band_widths(state_.config.sample_rate,
                                       state_.config.fft_size,
                                       state_.config.nb_erb,
                                       state_.config.min_nb_erb_freqs);
    if (dsp::erb_total_width(erb_widths_) != state_.config.freq_size()) {
        throw std::invalid_argument("ERB filterbank does not cover every frequency bin");
    }
    runner_->init_state(state_.recurrent);
}

void DeepFilterEngine::reset() {
    state_.reset();
    runner_->init_state(state_.recurrent);
    last_lsnr_ = 0.0f;
    last_stage_ = EnhancementStage::kBypass;
}

void DeepFilterEngine::set_post_filter_beta(float beta) {
    options_.post_filter_beta = std::max(0.0f, beta);
}

void DeepFilterEngine::set_attenuation_limit_db(float db) {
    options_.attenuation_limit_db = db;
}

EnhancementStage DeepFilterEngine::select_stage(float lsnr) const {
    if (!options_.enable_erb_stage) {
        return EnhancementStage::kBypass;
    }
    if (lsnr < options_.min_db_thresh) {
        return EnhancementStage::kSilence;
    }
    if (lsnr > options_.max_db_erb_thresh) {
        return EnhancementStage::kBypass;
    }
    if (lsnr > options_.max_db_df_thresh || !options_.enable_df_stage) {
        return EnhancementStage::kErbOnly;
    }
    return EnhancementStage::kErbAndDf;
}

void DeepFilterEngine::_push_feature_windows(const float* erb_feature,
                                             const float* complex_feature) {
    const ModelConfig& cfg = state_.config;
    const auto nb_erb = static_cast<std::size_t>(cfg.nb_erb);
    const auto nb_df = static_cast<std::size_t>(cfg.nb_df);

    // ERB window: [window][nb_erb].
    shift_and_append(state_.erb_window, nb_erb, erb_feature);

    // Complex feature window: [window][2][nb_df].
    std::vector<float> frame(2 * nb_df);
    std::memcpy(frame.data(), complex_feature, sizeof(float) * 2 * nb_df);
    shift_and_append(state_.spec_window, 2 * nb_df, frame.data());
}

void DeepFilterEngine::_push_decoder_windows(const EncoderResult& result) {
    const ModelConfig& cfg = state_.config;
    shift_and_append(
        state_.emb_window, static_cast<std::size_t>(cfg.erb_hidden()), result.emb.data());
    shift_and_append(state_.c0_window,
                     static_cast<std::size_t>(cfg.conv_ch) * static_cast<std::size_t>(cfg.nb_df),
                     result.c0.data());
}

void DeepFilterEngine::_gather_df_history() {
    const ModelConfig& cfg = state_.config;
    const auto n_freqs = static_cast<std::size_t>(cfg.freq_size());
    for (int k = 0; k < cfg.df_order; ++k) {
        std::copy(state_.rolling_x.frame(k),
                  state_.rolling_x.frame(k) + n_freqs,
                  state_.df_history.begin() +
                      static_cast<std::ptrdiff_t>(static_cast<std::size_t>(k) * n_freqs));
    }
}

void DeepFilterEngine::process_frame(const float* noisy, float* enhanced) {
    const ModelConfig& cfg = state_.config;
    const auto n_freqs = static_cast<std::size_t>(cfg.freq_size());
    const auto nb_df = static_cast<std::size_t>(cfg.nb_df);
    const auto hop = static_cast<std::size_t>(cfg.hop_size);

    // 1. Analysis and buffers.
    state_.stft.process(noisy, state_.spectrum.data());
    state_.rolling_y.push(state_.spectrum.data());
    state_.rolling_x.push(state_.spectrum.data());

    // 2. Features.
    dsp::feat_erb(state_.spectrum.data(),
                  erb_widths_,
                  cfg.norm_alpha,
                  state_.normalization.erb_state.data(),
                  state_.erb_feature.data());
    std::vector<float> complex_feature(2 * nb_df);
    dsp::feat_cplx_transposed(state_.spectrum.data(),
                              cfg.nb_df,
                              cfg.norm_alpha,
                              state_.normalization.unit_state.data(),
                              complex_feature.data());
    _push_feature_windows(state_.erb_feature.data(), complex_feature.data());

    // 3. Encoder inference.
    const int window = cfg.encoder_window_frames();
    const EncoderResult encoder = runner_->run_encoder(
        state_.erb_window.data(), window, state_.spec_window.data(), state_.recurrent);
    _push_decoder_windows(encoder);

    last_lsnr_ = encoder.lsnr;
    last_stage_ = select_stage(encoder.lsnr);

    // 4. Build the spectrum of the delayed output frame.
    const int output_index = cfg.df_order - 1;
    std::copy(state_.rolling_y.frame(output_index),
              state_.rolling_y.frame(output_index) + n_freqs,
              state_.enhanced.begin());

    switch (last_stage_) {
        case EnhancementStage::kSilence:
            std::fill(
                state_.enhanced.begin(), state_.enhanced.end(), std::complex<float>(0.0f, 0.0f));
            break;
        case EnhancementStage::kBypass:
            break;
        case EnhancementStage::kErbOnly:
        case EnhancementStage::kErbAndDf: {
            const std::vector<float>& mask = runner_->run_erb_decoder(encoder, state_.recurrent);
            dsp::erb_apply_interp_band_gain(state_.enhanced.data(), erb_widths_, mask.data());
            break;
        }
    }

    const bool gains_applied =
        last_stage_ == EnhancementStage::kErbOnly || last_stage_ == EnhancementStage::kErbAndDf;
    if (last_stage_ == EnhancementStage::kErbAndDf) {
        _gather_df_history();
        const std::vector<float>& coefs = runner_->run_df_decoder(
            state_.emb_window.data(), state_.c0_window.data(), state_.recurrent);
        const auto* coefficients = reinterpret_cast<const std::complex<float>*>(coefs.data());
        dsp::apply_deep_filter(state_.df_history.data(),
                               cfg.df_order,
                               coefficients,
                               cfg.nb_df,
                               cfg.freq_size(),
                               state_.enhanced.data());
    }

    // 5. Post filter and attenuation limit, both relative to the noisy spectrum of the
    //    output frame (libDF: the frame at `lookahead` samples from the newest one).
    const int lookahead = cfg.lookahead();
    const int noisy_index = state_.rolling_x.capacity_frames() - lookahead - 1;
    const std::complex<float>* noisy_spectrum = state_.rolling_x.frame(noisy_index);

    if (gains_applied && options_.post_filter_beta > 0.0f) {
        dsp::apply_post_filter(
            noisy_spectrum, state_.enhanced.data(), cfg.freq_size(), options_.post_filter_beta);
    }
    if (options_.attenuation_limit_db < 100.0f) {
        const float alpha = std::pow(10.0f, -std::abs(options_.attenuation_limit_db) / 20.0f);
        dsp::apply_attenuation_limit(
            noisy_spectrum, state_.enhanced.data(), cfg.freq_size(), alpha);
    }

    // 6. Synthesis.
    state_.istft.process(state_.enhanced.data(), enhanced);
    (void)hop;
}

}  // namespace dfn::enhancer
