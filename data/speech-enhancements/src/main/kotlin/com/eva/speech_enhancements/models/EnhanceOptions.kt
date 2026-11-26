package com.eva.speech_enhancements.models


data class EnhanceOptions(
    val blending: Blending = Blending(),
    val tuning: ModelTuning = ModelTuning(),
) {

    /**
     * @property strength Blend in `0.0..1.0`: `1.0` is the full model output, `0.0` the input.
     * @property adaptive Picks the blend per frame from the engine's local SNR estimate,
     *   protecting already-clean speech. [strength] acts as the ceiling.
     */
    data class Blending(
        val strength: Float = 1.0f,
        val adaptive: Boolean = false,
    ) {
        init {
            require(strength in 0.0f..1.0f) { "strength must be within 0.0..1.0" }
        }
    }

    /**
     * @property postFilterBeta Post-filter strength; `0.0` disables it (official CLI: `0.02`).
     * @property attenuationLimitDb Noise attenuation limit in dB; `>= 100.0` disables it.
     */
    data class ModelTuning(
        val postFilterBeta: Float = 0.02f,
        val attenuationLimitDb: Float = 100.0f,
    ) {
        init {
            require(postFilterBeta >= 0.0f) { "postFilterBeta must not be negative" }
        }
    }
}
