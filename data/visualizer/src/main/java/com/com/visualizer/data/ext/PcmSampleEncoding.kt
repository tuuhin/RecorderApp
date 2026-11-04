package com.com.visualizer.data.ext

internal enum class PcmSampleEncoding(val bytesPerSample: Int) {
    UNSIGNED_8(1),
    SIGNED_16(2),
    SIGNED_24(3),
    SIGNED_32(4),
    FLOAT_32(4),
}
