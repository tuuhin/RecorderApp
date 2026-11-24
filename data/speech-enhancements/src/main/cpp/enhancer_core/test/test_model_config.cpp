#include "model_config.h"

#include <gtest/gtest.h>

#include <cmath>
#include <stdexcept>
#include <string>

namespace dfn::enhancer {
namespace {

constexpr const char* kOfficialConfig = R"ini(
[train]
model = deepfilternet3

[df]
sr = 48000
fft_size = 960
hop_size = 480
nb_erb = 32
nb_df = 96
norm_tau = 1
lsnr_max = 35
lsnr_min = -15
min_nb_erb_freqs = 2
pad_mode = output
df_order = 5
df_lookahead = 2

[deepfilternet]
conv_lookahead = 2
conv_ch = 64
emb_hidden_dim = 256
emb_num_layers = 3
enc_linear_groups = 32
linear_groups = 16
mask_pf = False
df_hidden_dim = 256
df_num_layers = 2
df_gru_skip = groupedlinear
df_pathway_kernel_size_t = 5
enc_concat = False
conv_kernel_inp = 3,3
emb_gru_skip = none
df_n_iter = 1
convt_kernel = 1,3
lsnr_dropout = False
)ini";

TEST(ModelConfig, ParsesOfficialDeepFilterNet3Config) {
    const model_config config = model_config::from_ini_string(kOfficialConfig);
    EXPECT_EQ(config.sample_rate, 48000);
    EXPECT_EQ(config.fft_size, 960);
    EXPECT_EQ(config.hop_size, 480);
    EXPECT_EQ(config.nb_erb, 32);
    EXPECT_EQ(config.nb_df, 96);
    EXPECT_EQ(config.min_nb_erb_freqs, 2);
    EXPECT_EQ(config.df_order, 5);
    EXPECT_EQ(config.df_lookahead, 2);
    EXPECT_EQ(config.conv_lookahead, 2);
    EXPECT_EQ(config.conv_ch, 64);
    EXPECT_EQ(config.model_type, "deepfilternet3");
    EXPECT_NEAR(config.norm_alpha, 0.99f, 1e-7f);
    EXPECT_EQ(config.freq_size(), 481);
    EXPECT_EQ(config.erb_hidden(), 512);
    EXPECT_EQ(config.lookahead(), 2);
    EXPECT_EQ(config.latency_samples(), 1440);
}

TEST(ModelConfig, DerivesWindowsFromLookahead) {
    const model_config config = model_config::deepfilternet3_default();
    EXPECT_EQ(config.encoder_window_frames(), 3);
    EXPECT_EQ(config.erb_decoder_window_frames(), 1);
    EXPECT_EQ(config.df_decoder_window_frames(), 5);
}

TEST(ModelConfig, ComputesNormAlphaLikeLibdf) {
    // exp(-480/48000) = 0.9900498..., rounded to 0.99 at three decimals.
    EXPECT_NEAR(model_config::compute_norm_alpha(48000, 480, 1.0f), 0.99f, 1e-7f);
    // A ten times longer decay keeps four decimals.
    const float slow = model_config::compute_norm_alpha(48000, 480, 10.0f);
    EXPECT_GT(slow, 0.99f);
    EXPECT_LT(slow, 1.0f);
}

TEST(ModelConfig, ExplicitNormAlphaOverridesTau) {
    std::string text = kOfficialConfig;
    // `norm_alpha` belongs to the [df] section.
    const std::string anchor = "norm_tau = 1";
    text.replace(text.find(anchor), anchor.size(), anchor + "\nnorm_alpha = 0.95");
    const model_config config = model_config::from_ini_string(text);
    EXPECT_NEAR(config.norm_alpha, 0.95f, 1e-7f);
}

TEST(ModelConfig, AcceptsDeepFilterOrderInEitherSection) {
    const std::string text = R"ini(
[df]
sr = 48000
fft_size = 960
hop_size = 480
nb_erb = 32
nb_df = 96
[deepfilternet]
conv_lookahead = 1
df_order = 4
df_lookahead = 1
conv_ch = 32
model = deepfilternet3
[train]
model = deepfilternet3
)ini";
    const model_config config = model_config::from_ini_string(text);
    EXPECT_EQ(config.df_order, 4);
    EXPECT_EQ(config.df_lookahead, 1);
    EXPECT_EQ(config.conv_lookahead, 1);
}

TEST(ModelConfig, DefaultsMatchOfficialBaseline) {
    const model_config config = model_config::deepfilternet3_default();
    EXPECT_EQ(config.sample_rate, 48000);
    EXPECT_EQ(config.fft_size, 960);
    EXPECT_EQ(config.hop_size, 480);
    EXPECT_EQ(config.nb_erb, 32);
    EXPECT_EQ(config.nb_df, 96);
    EXPECT_EQ(config.df_order, 5);
    EXPECT_EQ(config.df_lookahead, 2);
    EXPECT_EQ(config.conv_lookahead, 2);
    EXPECT_EQ(config.model_type, "deepfilternet3");
}

TEST(ModelConfig, RejectsUnsupportedOrInvalidConfigurations) {
    model_config config = model_config::deepfilternet3_default();
    config.model_type = "deepfilternet2";
    EXPECT_THROW(config.validate(), std::invalid_argument);

    config = model_config::deepfilternet3_default();
    config.sample_rate = 16000;
    EXPECT_THROW(config.validate(), std::invalid_argument);

    config = model_config::deepfilternet3_default();
    config.hop_size = 960;
    EXPECT_THROW(config.validate(), std::invalid_argument);

    config = model_config::deepfilternet3_default();
    config.nb_df = 481;
    EXPECT_THROW(config.validate(), std::invalid_argument);

    config = model_config::deepfilternet3_default();
    config.norm_alpha = 1.0f;
    EXPECT_THROW(config.validate(), std::invalid_argument);

    EXPECT_NO_THROW(model_config::deepfilternet3_default().validate());
}

TEST(ModelConfig, MissingFileFallsBackToDefaultsWithError) {
    std::string error;
    const model_config config = model_config::from_ini_file("/nonexistent/config.ini", &error);
    EXPECT_FALSE(error.empty());
    EXPECT_EQ(config.fft_size, 960);
}

}  // namespace
}  // namespace dfn::enhancer