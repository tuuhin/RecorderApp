package com.eva.feature_editor.event

import com.eva.editor.domain.model.AudioClipConfig
import kotlin.time.Duration

sealed interface EditorScreenEvent {

	data object PlayAudio : EditorScreenEvent
	data object PauseAudio : EditorScreenEvent

	data class OnClipConfigChange(val config: AudioClipConfig) : EditorScreenEvent
	data class OnSeekTrack(val duration: Duration) : EditorScreenEvent

	data object CropSelectedArea : EditorScreenEvent
	data object RemoveSelectedArea : EditorScreenEvent
	data object ExportEditedMedia : EditorScreenEvent
}