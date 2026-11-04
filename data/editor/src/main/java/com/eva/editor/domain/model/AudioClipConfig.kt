package com.eva.editor.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class AudioClipConfig(
    val start: Duration = 0.seconds,
    val end: Duration = 1.seconds,
) {

    constructor(
        startMillis: Int,
        endMillis: Int,
    ) : this(startMillis.milliseconds, endMillis.milliseconds)

    fun validate(totalDuration: Duration): Boolean {
        return hasMinimumDuration && !start.isNegative() && end <= totalDuration
    }

    val hasMinimumDuration: Boolean
        get() = end - start >= MIN_CLIP_DURATION

    companion object {
        val MIN_CLIP_DURATION = 1.seconds
    }
}
