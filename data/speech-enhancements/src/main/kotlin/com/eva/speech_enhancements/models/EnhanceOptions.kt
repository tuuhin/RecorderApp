package com.eva.speech_enhancements.models

data class EnhanceOptions(
    /**
     * Enhancement blend in `0.0..1.0`: `1.0` is the full model output, `0.0` returns the
     * input unchanged, and values between blend the enhanced and the noisy sample
     * linearly. The native layer also clamps the value defensively.
     */
    val strength: Float = 1.0f,
) {
    init {
        require(strength in 0.0f..1.0f) { "strength must be within 0.0..1.0" }
    }
}