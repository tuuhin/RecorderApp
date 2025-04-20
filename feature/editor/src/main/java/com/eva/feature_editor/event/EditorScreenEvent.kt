package com.eva.feature_editor.event

sealed interface EditorScreenEvent {
	data object PlayAudio : EditorScreenEvent
	data object PauseAudio : EditorScreenEvent
}