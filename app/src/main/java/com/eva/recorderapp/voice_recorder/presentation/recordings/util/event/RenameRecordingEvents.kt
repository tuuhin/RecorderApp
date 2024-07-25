package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import androidx.compose.ui.text.input.TextFieldValue

sealed interface RenameRecordingEvents {

	data class OnTextValueChange(val textValue: TextFieldValue) : RenameRecordingEvents

	data object OnRenameRecording : RenameRecordingEvents

	data object OnCancelRenameRecording : RenameRecordingEvents

	data object OnShowRenameDialog : RenameRecordingEvents
}