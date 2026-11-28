#include "deepfilter_engine.h"

#include <algorithm>
#include <cmath>
#include <complex>
#include <cstddef>
#include <cstring>
#include <ranges>
#include <span>
#include <stdexcept>
#include <utility>
#include <vector>

#include "deep_filter.h"
#include "dsp_features.h"
#include "erb.h"

namespace dfn::enhancer {
namespace {

template <typename T>
void shift_and_append(std::vector<T>& window, std::size_t frame_length, std::span<const T> frame) {
    if (frame_length == 0 || window.size() < frame_length) {
        return;
    }
    const auto frame_span = frame.first(frame_length);
    auto win_span = std::span{window};

    if (win_span.size() > frame_length) {
        const std::size_t keep = win_span.size() - frame_length;
        // Left shift in place; std::memmove is safe for overlapping trivially copyable elements.
        std::memmove(win_span.data(), win_span.subspan(frame_length).data(), sizeof(T) * keep);
    }

    auto tail = win_span.subspan(win_span.size() - frame_length, frame_length);
    std::ranges::copy(frame_span, tail.begin());
}

}  // namespace

DeepFilterEngine::DeepFilterEngine(model_config config, std::shared_ptr<model_runner> runner,
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
    complex_feature_scratch_.assign(2 * static_cast<std::size_t>(state_.config.nb_df), 0.0f);
    runner_->init_state(state_.recurrent);
}

void DeepFilterEngine::reset() {
    state_.reset();
    std::ranges::fill(complex_feature_scratch_, 0.0f);
    runner_->init_state(state_.recurrent);
    last_lsnr_ = 0.0f;
    last_stage_ = EnhancementStage::kBypass;
}

void DeepFilterEngine::set_post_filter_beta(float beta) noexcept {
    options_.post_filter_beta = std::max(0.0f, beta);
}

void DeepFilterEngine::set_attenuation_limit_db(float db) noexcept {
    options_.attenuation_limit_db = db;
}

EnhancementStage DeepFilterEngine::select_stage(float lsnr) const noexcept {
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

void DeepFilterEngine::push_feature_windows(std::span<const float> erb_feature,
                                            std::span<const float> complex_feature) {
    const model_config& cfg = state_.config;
    const auto nb_erb = static_cast<std::size_t>(cfg.nb_erb);
    const auto complex_len = 2 * static_cast<std::size_t>(cfg.nb_df);

    // ERB window: [window][nb_erb].
    shift_and_append(state_.erb_window, nb_erb, erb_feature);

    // Complex feature window: [window][2][nb_df].
    shift_and_append(state_.spec_window, complex_len, complex_feature);
}

void DeepFilterEngine::push_decoder_windows(const encoder_result& result) {
    const model_config& cfg = state_.config;
    shift_and_append(
        state_.emb_window, static_cast<std::size_t>(cfg.erb_hidden()), std::span{result.emb});
    shift_and_append(state_.c0_window,
                     static_cast<std::size_t>(cfg.conv_ch) * static_cast<std::size_t>(cfg.nb_df),
                     std::span{result.c0});
}

void DeepFilterEngine::gather_df_history() {
    const model_config& cfg = state_.config;
    const auto n_freqs = static_cast<std::size_t>(cfg.freq_size());
    const auto df_order = static_cast<std::size_t>(cfg.df_order);
    auto history_span = std::span{state_.df_history};

    for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{0}, df_order)) {
        const auto frame_span = std::span{state_.rolling_x.frame(static_cast<int>(k)), n_freqs};
        auto dst = history_span.subspan(k * n_freqs, n_freqs);
        std::ranges::copy(frame_span, dst.begin());
    }
}

void DeepFilterEngine::process_frame(std::span<const float> noisy, std::span<float> enhanced) {
    const model_config& cfg = state_.config;
    const auto n_freqs = static_cast<std::size_t>(cfg.freq_size());
    const auto nb_df = static_cast<std::size_t>(cfg.nb_df);
    const auto df_order = static_cast<std::size_t>(cfg.df_order);
    const auto hop = static_cast<std::size_t>(cfg.hop_size);

    const auto in_hop = noisy.first(hop);
    auto out_hop = enhanced.first(hop);
    auto spec_span = std::span{state_.spectrum}.first(n_freqs);
    auto enh_span = std::span{state_.enhanced}.first(n_freqs);

    // 1. Analysis and buffers.
    state_.stft.process(in_hop.data(), spec_span.data());
    state_.rolling_y.push(spec_span.data());
    state_.rolling_x.push(spec_span.data());

    // 2. Features.
    dsp::feat_erb(
        spec_span, erb_widths_, cfg.norm_alpha, state_.normalization.erb_state, state_.erb_feature);

    auto complex_feat_span = std::span{complex_feature_scratch_}.first(2 * nb_df);
    dsp::feat_cplx_transposed(
        spec_span, nb_df, cfg.norm_alpha, state_.normalization.unit_state, complex_feat_span);
    push_feature_windows(state_.erb_feature, complex_feat_span);

    // 3. Encoder inference.
    const int window = cfg.encoder_window_frames();
    const encoder_result encoder = runner_->run_encoder(
        std::span{state_.erb_window}, window, std::span{state_.spec_window}, state_.recurrent);
    push_decoder_windows(encoder);

    last_lsnr_ = encoder.lsnr;
    last_stage_ = select_stage(encoder.lsnr);

    // 4. Build the spectrum of the delayed output frame.
    const int output_index = cfg.df_order - 1;
    const auto delayed_frame = std::span{state_.rolling_y.frame(output_index), n_freqs};
    std::ranges::copy(delayed_frame, enh_span.begin());

    switch (last_stage_) {
        case EnhancementStage::kSilence:
            std::ranges::fill(enh_span, std::complex<float>{0.0f, 0.0f});
            break;
        case EnhancementStage::kBypass:
            break;
        case EnhancementStage::kErbOnly:
        case EnhancementStage::kErbAndDf: {
            const std::vector<float>& mask = runner_->run_erb_decoder(encoder, state_.recurrent);
            dsp::erb_apply_interp_band_gain(enh_span, erb_widths_, mask);
            break;
        }
    }

    const bool gains_applied =
        last_stage_ == EnhancementStage::kErbOnly || last_stage_ == EnhancementStage::kErbAndDf;
    if (last_stage_ == EnhancementStage::kErbAndDf) {
        gather_df_history();
        const std::vector<float>& coefs = runner_->run_df_decoder(
            std::span{state_.emb_window}, std::span{state_.c0_window}, state_.recurrent);
        const auto coef_complex =
            std::span{reinterpret_cast<const std::complex<float>*>(coefs.data()), df_order * nb_df};
        dsp::apply_deep_filter(state_.df_history, df_order, coef_complex, nb_df, n_freqs, enh_span);
    }

    // 5. Post filter and attenuation limit, both relative to the noisy spectrum of the
    //    output frame (libDF: the frame at `lookahead` samples from the newest one).
    const int lookahead = cfg.lookahead();
    const int noisy_index = state_.rolling_x.capacity_frames() - lookahead - 1;
    const auto noisy_spectrum = std::span{state_.rolling_x.frame(noisy_index), n_freqs};

    if (gains_applied && options_.post_filter_beta > 0.0f) {
        dsp::apply_post_filter(noisy_spectrum, enh_span, n_freqs, options_.post_filter_beta);
    }
    if (options_.attenuation_limit_db < 100.0f) {
        const float alpha = std::pow(10.0f, -std::abs(options_.attenuation_limit_db) / 20.0f);
        dsp::apply_attenuation_limit(noisy_spectrum, enh_span, n_freqs, alpha);
    }

    // 6. Synthesis.
    state_.istft.process(enh_span.data(), out_hop.data());
}

}  // namespace dfn::enhancer
