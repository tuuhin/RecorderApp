package com.eva.speech_enhancements.models


sealed class EnhanceOptions(
    open val blending: Blending,
    open val tuning: ModelTuning,
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

    /** Light touch for already-quiet rooms, mic hiss, or subtle ambience preservation. */
    data object QuietRoom : EnhanceOptions(
        blending = Blending(strength = 0.85f, adaptive = true),
        tuning = ModelTuning(postFilterBeta = 0.01f, attenuationLimitDb = 18.0f),
    )

    /** Balanced default for offices with fans, AC hum, typing, or light chatter. */
    data object OpenOffice : EnhanceOptions(
        blending = Blending(strength = 1.0f, adaptive = true),
        tuning = ModelTuning(postFilterBeta = 0.025f, attenuationLimitDb = 28.0f),
    )

    /** Aggressive suppression for busy streets, traffic, wind, or heavy background noise. */
    data object Street : EnhanceOptions(
        blending = Blending(strength = 1.0f, adaptive = false),
        tuning = ModelTuning(postFilterBeta = 0.05f, attenuationLimitDb = 100.0f),
    )

    /** Protects vocal clarity and natural dynamics for podcasts, interviews, and voiceovers. */
    data object Podcast : EnhanceOptions(
        blending = Blending(strength = 0.9f, adaptive = true),
        tuning = ModelTuning(postFilterBeta = 0.015f, attenuationLimitDb = 20.0f),
    )

    /** Handles fluctuating background chatter and clatter without harsh silence gating. */
    data object Cafe : EnhanceOptions(
        blending = Blending(strength = 1.0f, adaptive = true),
        tuning = ModelTuning(postFilterBeta = 0.035f, attenuationLimitDb = 30.0f),
    )

    /** Low-latency friendly preset for live calls where consistent noise floor matters. */
    data object VoiceCall : EnhanceOptions(
        blending = Blending(strength = 1.0f, adaptive = true),
        tuning = ModelTuning(postFilterBeta = 0.02f, attenuationLimitDb = 22.0f),
    )

    /** Full manual control over blending and model tuning parameters. */
    data class Custom(
        override val blending: Blending = Blending(),
        override val tuning: ModelTuning = ModelTuning(),
    ) : EnhanceOptions(blending = blending, tuning = tuning)
}
