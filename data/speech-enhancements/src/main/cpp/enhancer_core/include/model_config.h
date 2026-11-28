#ifndef DFN_ENHANCER_MODEL_CONFIG_H_
#define DFN_ENHANCER_MODEL_CONFIG_H_

// DeepFilterNet model configuration, loaded from the `config.ini` shipped with the
// official ONNX model bundle.
//
// Reference: DeepFilterNet/df/config.py (DfParams, ModelParams) and
// libDF/src/tract.rs (calc_norm_alpha, lookahead computation).
//
// The DSP layer must never hardcode these values; every processing component is driven by
// the values in this struct.

#include <algorithm>
#include <map>
#include <string>
#include <string_view>

namespace dfn::enhancer {

struct model_config {
    // [df]
    int sample_rate = 48000;
    int fft_size = 960;
    int hop_size = 480;
    int nb_erb = 32;
    int nb_df = 96;
    int min_nb_erb_freqs = 2;
    int df_order = 5;
    int df_lookahead = 2;
    float norm_tau = 1.0f;

    // [deepfilternet]
    int conv_lookahead = 2;
    int conv_ch = 64;

    // [train]
    std::string model_type = "deepfilternet3";

    // Derived.
    float norm_alpha = 0.99f;

    [[nodiscard]] constexpr int freq_size() const noexcept { return fft_size / 2 + 1; }

    // Width of the flattened encoder embedding: conv_ch * nb_erb / 4 (512 for DFN3).
    [[nodiscard]] constexpr int erb_hidden() const noexcept { return conv_ch * nb_erb / 4; }

    // Number of output frames by which the model output is delayed.
    [[nodiscard]] constexpr int lookahead() const noexcept {
        return std::max(df_lookahead, conv_lookahead);
    }

    // Total algorithmic latency in samples: model lookahead plus the STFT window delay.
    [[nodiscard]] constexpr int latency_samples() const noexcept {
        return lookahead() * hop_size + (fft_size - hop_size);
    }

    // Number of input frames each graph needs per call (causal convolution history + 1).
    [[nodiscard]] constexpr int encoder_window_frames() const noexcept {
        return conv_lookahead + 1;
    }
    [[nodiscard]] constexpr int erb_decoder_window_frames() const noexcept { return 1; }
    [[nodiscard]] static constexpr int df_decoder_window_frames() noexcept { return 4 + 1; }

    // Alpha of the exponential feature normalization. Mirrors libDF's calc_norm_alpha
    // (rounding to increasing decimal precision until the factor drops below one).
    [[nodiscard]] static float compute_norm_alpha(int sample_rate, int hop_size, float tau);

    // Parses the upstream `config.ini` text. Unknown sections/keys are ignored.
    [[nodiscard]] static model_config from_ini_string(std::string_view text);

    // Reads and parses a `config.ini` file. Returns the default DeepFilterNet 3
    // configuration when the file cannot be read (with `error` set).
    [[nodiscard]] static model_config from_ini_file(std::string_view path,
                                                    std::string* error = nullptr);

    // Official DeepFilterNet 3 baseline (see models/DeepFilterNet3_onnx.tar.gz/config.ini).
    [[nodiscard]] static model_config deepfilternet3_default();

    // Throws std::invalid_argument when the configuration cannot be processed.
    void validate() const;

   private:
    void apply_section(std::string_view section, const std::map<std::string, std::string>& values);
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_MODEL_CONFIG_H_
