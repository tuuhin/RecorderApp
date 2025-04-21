package com.eva.feature_editor.event

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioClipConfig(
	val start: Duration = 0.seconds,
	val end: Duration = 1.seconds,
)