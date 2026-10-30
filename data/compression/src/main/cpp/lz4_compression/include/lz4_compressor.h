#pragma once

#include <cstdint>
#include <span>
#include <vector>

namespace lz4 {
constexpr std::size_t kMaxGrownOutputBytes = 256 * 1024 * 1024;
}

class lz4_compressor {

public:
    [[nodiscard]]
    static std::vector<std::uint8_t> compress_frame(std::span<const std::uint8_t> source);

    [[nodiscard]]
    static std::vector<std::uint8_t> decompress_frame(std::span<const std::uint8_t> source);
};
