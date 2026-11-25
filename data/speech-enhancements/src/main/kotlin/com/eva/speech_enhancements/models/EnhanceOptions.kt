package com.eva.speech_enhancements.models

/**
 * Configuration options for speech enhancement.
 *
 * @property strength Enhancement blend in `0.0..1.0`: `1.0` is the full model output, `0.0` returns
 * the input unchanged, and values between blend the enhanced and the noisy sample linearly. The
 * native layer also clamps the value defensively.
 * @property adaptiveStrength Chooses the blend per frame from the engine's local SNR estimate
 * instead of applying [strength] uniformly: noisy frames keep the full model output, already-clean
 * frames fade back towards the input. [strength] then acts as the ceiling. Guards against the
 * model over-suppressing clean speech.
 * @property postFilterBeta Post-filter (Valin et al.) strength applied after the mask/deep-filter
 * stages. `0.0` disables it; the official Python CLI uses `0.02`. A small beta trades a little
 * extra noise for a smoother, less "musical" residual.
 * @property attenuationLimitDb Noise attenuation limit in dB: the output is mixed back towards the
 * input so the residual noise is never attenuated by more than this. Values `>= 100.0` disable the
 * limit (full suppression).
 */
data class EnhanceOptions(
    val strength: Float = 1.0f,
    val adaptiveStrength: Boolean = false,
    val postFilterBeta: Float = 0.02f,
    val attenuationLimitDb: Float = 100.0f,
) {
    init {
        require(strength in 0.0f..1.0f) { "strength must be within 0.0..1.0" }
        require(postFilterBeta >= 0.0f) { "postFilterBeta must not be negative" }
    }
}
