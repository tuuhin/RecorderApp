package com.eva.speech_enhancements

import com.eva.speech_enhancements.models.EnhanceOptions

/**
 * Runs a whole clip through the streaming [IClearSpeechEnhancer] API: feeds [samples] in
 * chunks of [chunkSize], then finishes the stream. The concatenation of everything
 * returned is exactly [samples.size] long and delay compensated, so this is the
 * streaming replacement of the old one-shot `enhance` call.
 *
 * @param chunkSize chunk length in samples; arbitrary chunking must not change the output.
 */
suspend fun IClearSpeechEnhancer.enhanceOffline(
    samples: FloatArray,
    chunkSize: Int = 48_000,
    options: EnhanceOptions = EnhanceOptions(),
): FloatArray {
    require(chunkSize > 0) { "chunkSize must be positive" }

    val out = FloatArray(samples.size)
    var written = 0

    var start = 0
    while (start < samples.size) {
        val end = minOf(start + chunkSize, samples.size)
        val available = process(samples.copyOfRange(start, end), options)
        check(written + available.size <= out.size) { "stream returned more samples than fed" }
        available.copyInto(out, written)
        written += available.size
        start = end
    }

    val tail = finish(options)
    check(written + tail.size == out.size) {
        "stream returned ${written + tail.size} samples for ${out.size} input samples"
    }
    tail.copyInto(out, written)
    return out
}
