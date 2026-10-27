#include <jni.h>

#include "wav_recorder.h"

#include <exception>
#include <filesystem>
#include <memory>
#include <stdexcept>
#include <string>
#include <utility>

class UtfChars final {
   public:
    UtfChars(JNIEnv* env, jstring value) : _jEnv(env), _jValue(value) {
        if (_jValue == nullptr) throw std::invalid_argument("File path must not be null");
        _cChars = _jEnv->GetStringUTFChars(_jValue, nullptr);
        if (_cChars == nullptr) throw std::runtime_error("Failed to access Java string");
    }

    ~UtfChars() {
        if (_cChars != nullptr) _jEnv->ReleaseStringUTFChars(_jValue, _cChars);
    }

    UtfChars(const UtfChars&) = delete;
    UtfChars& operator=(const UtfChars&) = delete;

    [[nodiscard]] const char* get() const noexcept { return _cChars; }

   private:
    JNIEnv* _jEnv;
    jstring _jValue;
    const char* _cChars = nullptr;
};
