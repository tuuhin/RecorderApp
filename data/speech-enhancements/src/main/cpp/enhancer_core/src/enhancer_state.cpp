#include "enhancer_state.h"

#include <algorithm>
#include <cstring>

namespace dfn::enhancer {

FrameRing::FrameRing(int capacity_frames, int frame_size)
    : capacity_frames_(capacity_frames), frame_size_(frame_size) {
    data_.assign(static_cast<std::size_t>(capacity_frames_ * frame_size_),
                 std::complex<float>(0.0f, 0.0f));
}

void FrameRing::reset() {
    std::fill(data_.begin(), data_.end(), std::complex<float>(0.0f, 0.0f));
}

void FrameRing::push(const std::complex<float>* frame) {
    if (capacity_frames_ <= 0 || frame_size_ <= 0) {
        return;
    }
    const auto size = static_cast<std::size_t>(frame_size_);
    if (capacity_frames_ > 1) {
        std::memmove(
            data_.data(),
            data_.data() + size,
            sizeof(std::complex<float>) * size * static_cast<std::size_t>(capacity_frames_ - 1));
    }
    std::copy(frame,
              frame + size,
              data_.begin() + static_cast<std::ptrdiff_t>(
                                  static_cast<std::size_t>(capacity_frames_ - 1) * size));
}

EnhancerState::EnhancerState(model_config model_config)
    : config(std::move(model_config)),
      stft(dsp::stft_config{config.fft_size, config.hop_size}),
      istft(dsp::stft_config{config.fft_size, config.hop_size}),
      rolling_y(config.df_order + config.conv_lookahead, config.freq_size()),
      rolling_x(std::max(config.df_order, config.lookahead()), config.freq_size()) {
    const auto window = static_cast<std::size_t>(config.encoder_window_frames());
    const auto df_window = static_cast<std::size_t>(config.df_decoder_window_frames());
    erb_window.assign(window * static_cast<std::size_t>(config.nb_erb), 0.0f);
    spec_window.assign(2 * window * static_cast<std::size_t>(config.nb_df), 0.0f);
    emb_window.assign(df_window * static_cast<std::size_t>(config.erb_hidden()), 0.0f);
    c0_window.assign(df_window * static_cast<std::size_t>(config.conv_ch) *
                         static_cast<std::size_t>(config.nb_df),
                     0.0f);
    spectrum.assign(static_cast<std::size_t>(config.freq_size()), {0.0f, 0.0f});
    enhanced.assign(static_cast<std::size_t>(config.freq_size()), {0.0f, 0.0f});
    df_history.assign(static_cast<std::size_t>(config.df_order * config.freq_size()), {0.0f, 0.0f});
    erb_feature.assign(static_cast<std::size_t>(config.nb_erb), 0.0f);
    df_coefs.assign(static_cast<std::size_t>(config.df_order * config.nb_df * 2), 0.0f);
    normalization.reset(config.nb_erb, config.nb_df);
}

void EnhancerState::reset() {
    stft.reset();
    istft.reset();
    normalization.reset(config.nb_erb, config.nb_df);
    recurrent.clear();
    std::fill(erb_window.begin(), erb_window.end(), 0.0f);
    std::fill(spec_window.begin(), spec_window.end(), 0.0f);
    std::fill(emb_window.begin(), emb_window.end(), 0.0f);
    std::fill(c0_window.begin(), c0_window.end(), 0.0f);
    rolling_y.reset();
    rolling_x.reset();
    std::fill(spectrum.begin(), spectrum.end(), std::complex<float>(0.0f, 0.0f));
    std::fill(enhanced.begin(), enhanced.end(), std::complex<float>(0.0f, 0.0f));
    std::fill(df_history.begin(), df_history.end(), std::complex<float>(0.0f, 0.0f));
    std::fill(erb_feature.begin(), erb_feature.end(), 0.0f);
    std::fill(df_coefs.begin(), df_coefs.end(), 0.0f);
}

}  // namespace dfn::enhancer
