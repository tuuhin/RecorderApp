package com.eva.feature_editor.event

import com.eva.editor.data.AudioClipConfig
import kotlin.time.Duration

sealed interface EditorScreenEvent {

	data object PlayAudio : EditorScreenEvent
	data object PauseAudio : EditorScreenEvent

	data class OnClipConfigChange(val config: AudioClipConfig) : EditorScreenEvent
	data class OnSeekTrack(val duration: Duration) : EditorScreenEvent

	data object TrimSelectedArea : EditorScreenEvent
}