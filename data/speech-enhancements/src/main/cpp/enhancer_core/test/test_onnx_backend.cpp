#include "onnx_backend.h"

#include <gtest/gtest.h>

#include <cmath>
#include <fstream>
#include <memory>
#include <string>
#include <vector>

#include "enhancer_session.h"
#include "model_config.h"
#include "support/reference.h"

#ifndef DFN_TEST_MODEL_DIR
#define DFN_TEST_MODEL_DIR ""
#endif
#ifndef DFN_TEST_ENHANCER_DATA
#define DFN_TEST_ENHANCER_DATA ""
#endif

namespace dfn::enhancer {
namespace {

std::string model_path(const std::string& name) {
    const std::string dir = DFN_TEST_MODEL_DIR;
    return dir.empty() ? std::string() : dir + "/" + name;
}

bool models_available() {
    const std::string dir = DFN_TEST_MODEL_DIR;
    if (dir.empty()) {
        return false;
    }
    std::ifstream probe(dir + "/enc_stream.onnx", std::ios::binary);
    return probe.good();
}

bool reference_available() {
    const std::string dir = DFN_TEST_ENHANCER_DATA;
    if (dir.empty()) {
        return false;
    }
    std::ifstream probe(dir + "/audio.f32", std::ios::binary);
    return probe.good();
}

std::shared_ptr<OnnxBackend> load_backend(const ModelConfig& config, std::string* error) {
    return OnnxBackend::from_files(model_path("enc_stream.onnx"),
                                   model_path("erb_dec_stream.onnx"),
                                   model_path("df_dec_stream.onnx"),
                                   config,
                                   OnnxBackend::Options{},
                                   error);
}

TEST(OnnxBackend, LoadsAllThreeGraphsAndInspectsTheirSignatures) {
    if (!models_available()) {
        GTEST_SKIP() << "set DFN_TEST_MODEL_DIR to a directory with *_stream.onnx";
    }
    const ModelConfig config = ModelConfig::deepfilternet3_default();
    std::string error;
    auto backend = load_backend(config, &error);
    ASSERT_NE(backend, nullptr) << error;

    // Encoder: two feature inputs plus one recurrent state input.
    ASSERT_EQ(backend->encoder_inputs().size(), 3u);
    EXPECT_EQ(backend->encoder_inputs()[0].name, "feat_erb");
    const std::vector<int64_t>& erb_shape = backend->encoder_inputs()[0].shape;
    ASSERT_EQ(erb_shape.size(), 4u);
    EXPECT_EQ(erb_shape[0], 1);
    EXPECT_EQ(erb_shape[1], 1);
    EXPECT_TRUE(erb_shape[2] < 0 || erb_shape[2] == 3);
    EXPECT_EQ(erb_shape[3], 32);
    EXPECT_EQ(backend->encoder_inputs()[1].name, "feat_spec");
    // The time axis stays dynamic in the prepared graph (a Slice replaces the static
    // convolution padding), so only the static dimensions are asserted.
    const std::vector<int64_t>& spec_shape = backend->encoder_inputs()[1].shape;
    ASSERT_EQ(spec_shape.size(), 4u);
    EXPECT_EQ(spec_shape[0], 1);
    EXPECT_EQ(spec_shape[1], 2);
    EXPECT_TRUE(spec_shape[2] < 0 || spec_shape[2] == 3);
    EXPECT_EQ(spec_shape[3], 96);
    EXPECT_EQ(backend->encoder_inputs()[2].name, "state_in_0");
    EXPECT_EQ(backend->encoder_inputs()[2].shape, (std::vector<int64_t>{1, 1, 256}));

    // ERB decoder and DF decoder each have two recurrent layers.
    EXPECT_EQ(backend->erb_decoder_inputs().size(), 7u);
    EXPECT_EQ(backend->df_decoder_inputs().size(), 4u);
}

TEST(OnnxBackend, RejectsGraphsThatDoNotMatchTheModelConfig) {
    if (!models_available()) {
        GTEST_SKIP() << "set DFN_TEST_MODEL_DIR to a directory with *_stream.onnx";
    }
    ModelConfig config = ModelConfig::deepfilternet3_default();
    config.nb_erb = 64;  // the graphs were exported with 32 ERB bands
    std::string error;
    auto backend = load_backend(config, &error);
    EXPECT_EQ(backend, nullptr);
    EXPECT_FALSE(error.empty());
    EXPECT_NE(error.find("feat_erb"), std::string::npos) << error;
}

TEST(OnnxBackend, EncoderProducesConfiguredShapesAndAdvancesState) {
    if (!models_available()) {
        GTEST_SKIP() << "set DFN_TEST_MODEL_DIR to a directory with *_stream.onnx";
    }
    const ModelConfig config = ModelConfig::deepfilternet3_default();
    std::string error;
    auto backend = load_backend(config, &error);
    ASSERT_NE(backend, nullptr) << error;

    RecurrentState state;
    backend->init_state(state);
    ASSERT_EQ(state.encoder.size(), 1u);
    ASSERT_EQ(state.erb_decoder.size(), 2u);
    ASSERT_EQ(state.df_decoder.size(), 2u);

    const int window = config.encoder_window_frames();
    std::vector<float> erb(static_cast<std::size_t>(window * config.nb_erb), 0.0f);
    std::vector<float> spec(static_cast<std::size_t>(window * 2 * config.nb_df), 0.0f);
    // Non-trivial input so the recurrent state must change.
    for (std::size_t i = 0; i < erb.size(); ++i) {
        erb[i] = 0.5f + 0.01f * static_cast<float>(i);
    }
    for (std::size_t i = 0; i < spec.size(); ++i) {
        spec[i] = 0.1f * static_cast<float>(i % 13) - 0.5f;
    }

    const EncoderResult first = backend->run_encoder(erb.data(), window, spec.data(), state);
    EXPECT_EQ(first.e0.size(), static_cast<std::size_t>(config.conv_ch * config.nb_erb));
    EXPECT_EQ(first.e1.size(), static_cast<std::size_t>(config.conv_ch * config.nb_erb / 2));
    EXPECT_EQ(first.e2.size(), static_cast<std::size_t>(config.conv_ch * config.nb_erb / 4));
    EXPECT_EQ(first.e3.size(), static_cast<std::size_t>(config.conv_ch * config.nb_erb / 4));
    EXPECT_EQ(first.emb.size(), static_cast<std::size_t>(config.erb_hidden()));
    EXPECT_EQ(first.c0.size(), static_cast<std::size_t>(config.conv_ch * config.nb_df));
    EXPECT_GT(std::abs(first.lsnr), 0.0f);

    float state_norm = 0.0f;
    for (const float value : state.encoder[0]) {
        state_norm += std::abs(value);
    }
    EXPECT_GT(state_norm, 0.0f) << "the recurrent state must be non-zero after a frame";

    const std::vector<float>& mask = backend->run_erb_decoder(first, state);
    EXPECT_EQ(mask.size(), static_cast<std::size_t>(config.nb_erb));
    for (const float value : mask) {
        EXPECT_GE(value, 0.0f);
        EXPECT_LE(value, 1.0f);
    }

    std::vector<float> emb_window(
        static_cast<std::size_t>(config.df_decoder_window_frames() * config.erb_hidden()), 0.0f);
    std::vector<float> c0_window(
        static_cast<std::size_t>(config.df_decoder_window_frames() * config.conv_ch * config.nb_df),
        0.0f);
    const std::vector<float>& coefs =
        backend->run_df_decoder(emb_window.data(), c0_window.data(), state);
    EXPECT_EQ(coefs.size(), static_cast<std::size_t>(config.df_order * config.nb_df * 2));
}

TEST(OnnxBackend, EndToEndSessionMatchesThePythonStreamingReference) {
    if (!models_available()) {
        GTEST_SKIP() << "set DFN_TEST_MODEL_DIR to a directory with *_stream.onnx";
    }
    if (!reference_available()) {
        GTEST_SKIP() << "set DFN_TEST_ENHANCER_DATA to the generated reference directory";
    }
    const std::string data = DFN_TEST_ENHANCER_DATA;
    const ModelConfig config = ModelConfig::deepfilternet3_default();

    const std::vector<float> signal = test::load_f32_in(data, "signal");
    const std::vector<float> expected_audio = test::load_f32_in(data, "audio");
    ASSERT_FALSE(signal.empty());

    std::string error;
    auto backend = load_backend(config, &error);
    ASSERT_NE(backend, nullptr) << error;

    EnhancerSession session(backend, config, EnhancerOptions{});
    std::vector<float> output;
    session.process(signal.data(), signal.size(), output);
    session.flush(output);

    // Drop the algorithmic latency. The reference "audio" starts at the window centre
    // (fft_size - hop_size samples into the synthesis), one hop later than the session's
    // aligned output.
    const auto latency = static_cast<std::size_t>(session.latency_samples());
    ASSERT_GT(output.size(), latency);
    const std::vector<float> aligned(output.begin() + static_cast<std::ptrdiff_t>(latency),
                                     output.end());
    const std::size_t compare = std::min(
        aligned.size(),
        expected_audio.size() - static_cast<std::size_t>(config.fft_size - config.hop_size));
    ASSERT_GT(compare, 0u);
    const std::size_t offset = static_cast<std::size_t>(config.fft_size - config.hop_size);
    const test::Metrics metrics =
        test::compute_metrics(expected_audio.data() + offset, aligned.data(), compare);
    test::expect_max_abs("end-to-end enhanced audio vs Python streaming reference", metrics, 2e-4f);

    EXPECT_GE(session.stats().frames_processed, 1u);
}

TEST(OnnxBackend, PerFrameLsnrMatchesTheReference) {
    if (!models_available() || !reference_available()) {
        GTEST_SKIP() << "models or reference data unavailable";
    }
    const std::string data = DFN_TEST_ENHANCER_DATA;
    const ModelConfig config = ModelConfig::deepfilternet3_default();
    const std::vector<float> expected_lsnr = test::load_f32_in(data, "lsnr");
    ASSERT_FALSE(expected_lsnr.empty());

    std::string error;
    auto backend = load_backend(config, &error);
    ASSERT_NE(backend, nullptr) << error;

    std::vector<float> observed;
    // Run the engine hop by hop and record the local SNR reported for every frame.
    const std::vector<float> signal = test::load_f32_in(data, "signal");
    EnhancerSession session(backend, config, EnhancerOptions{});
    std::vector<float> ignore;
    std::vector<float> chunk(signal.begin(), signal.begin() + signal.size());
    // Process hop by hop and capture the lsnr after each call.
    const auto hop = static_cast<std::size_t>(config.hop_size);
    for (std::size_t offset = 0; offset + hop <= chunk.size(); offset += hop) {
        session.process(chunk.data() + offset, hop, ignore);
        observed.push_back(session.stats().last_lsnr);
    }
    ASSERT_GE(observed.size(), expected_lsnr.size());
    const test::Metrics metrics =
        test::compute_metrics(expected_lsnr.data(), observed.data(), expected_lsnr.size());
    test::expect_max_abs("per-frame local SNR vs Python streaming reference", metrics, 5e-3f);
}

}  // namespace
}  // namespace dfn::enhancer
