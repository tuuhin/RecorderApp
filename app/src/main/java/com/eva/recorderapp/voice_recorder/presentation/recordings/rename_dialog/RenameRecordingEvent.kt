package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.compose.ui.text.input.TextFieldValue

sealed interface RenameRecordingEvent {

	data class OnTextValueChange(val textValue: TextFieldValue) : RenameRecordingEvent

	data object OnRenameRecording : RenameRecordingEvent

}