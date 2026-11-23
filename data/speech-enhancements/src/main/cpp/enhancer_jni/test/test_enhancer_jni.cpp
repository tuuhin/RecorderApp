#include "enhancer_jni.h"
#include "speech_enhancer_manager.h"

#include <gtest/gtest.h>

#include <cstring>
#include <span>
#include <stdexcept>
#include <string>
#include <vector>

namespace dfn::jni {
namespace {

TEST(EnhancerJni, ExposesBuildVersion) {
    const char* version = native_engine_version();
    ASSERT_NE(version, nullptr);
    EXPECT_GT(std::strlen(version), 0u);
}

// A manager pointing at a model home without the prepared graphs must fail closed with a
// reported reason on every path, never crash: this is the state every fresh checkout of
// the app is in until the model files are shipped or downloaded.
TEST(EnhancerJni, AbsentModelDirectoryIsRejectedNotFatal) {
    SpeechEnhancerManager manager("/nonexistent-dfn-model-dir");

    EXPECT_FALSE(manager.prepare());

    // The pipeline constants are still readable from the parsed-or-defaulted config.
    EXPECT_EQ(manager.latency_samples(), 1440);

    std::vector<float> noise(4800, 0.25f);
    EXPECT_THROW(manager.enhance(noise, kModelSampleRate, 1.0f), std::runtime_error);

    // A wrong sample rate is an argument error before any model is touched.
    EXPECT_THROW(manager.enhance(noise, 44100, 1.0f), std::invalid_argument);
}

// The manager holds no static state: every instance owns its config, engine and output
// buffer, so multiple instances coexist without interference (the app would create one).
TEST(EnhancerJni, InstancesAreIndependent) {
    SpeechEnhancerManager first("/nonexistent-dfn-model-dir");
    SpeechEnhancerManager second("/nonexistent-dfn-model-dir");

    EXPECT_EQ(first.latency_samples(), second.latency_samples());

    std::vector<float> noise(4800, 0.25f);
    EXPECT_THROW(first.enhance(noise, kModelSampleRate, 1.0f), std::runtime_error);
    EXPECT_THROW(second.enhance(noise, kModelSampleRate, 1.0f), std::runtime_error);

    // One instance's failed calls do not poison the other.
    EXPECT_FALSE(first.prepare());
    EXPECT_FALSE(second.prepare());
}

}  // namespace
}  // namespace dfn::jni