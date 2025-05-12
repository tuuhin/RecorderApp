package com.eva.feature_editor.event

import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import kotlin.time.Duration

sealed interface EditorScreenEvent {

	data object PlayAudio : EditorScreenEvent
	data object PauseAudio : EditorScreenEvent

	data class OnClipConfigChange(val config: AudioClipConfig) : EditorScreenEvent
	data class OnSeekTrack(val duration: Duration) : EditorScreenEvent

	data class OnEditAction(val action: AudioEditAction) : EditorScreenEvent

	data object OnUndoEdit : EditorScreenEvent
	data object OnRedoEdit : EditorScreenEvent

	data object BeginTransformation : EditorScreenEvent
	data object OnDismissExportSheet : EditorScreenEvent
	data object OnSaveExportFile : EditorScreenEvent
}