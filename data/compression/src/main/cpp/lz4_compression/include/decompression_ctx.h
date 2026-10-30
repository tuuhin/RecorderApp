
#include <android/log.h>
#include <lz4frame.h>

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <vector>

class de_compression_context {
public:
    de_compression_context() {
        const auto result = LZ4F_createDecompressionContext(&_ctx, LZ4F_VERSION);
        if (!LZ4F_isError(result)) return;
        throw std::runtime_error(LZ4F_getErrorName(result));
    }

    ~de_compression_context() {
        if (_ctx == nullptr) return;
        LZ4F_freeDecompressionContext(_ctx);
    }

    de_compression_context(const de_compression_context&) = delete;

    de_compression_context& operator=(const de_compression_context&) = delete;

    [[nodiscard]] LZ4F_dctx* get() const noexcept { return _ctx; }

private:
    LZ4F_dctx* _ctx = nullptr;
};
