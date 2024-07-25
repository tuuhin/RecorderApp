package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import androidx.compose.ui.text.input.TextFieldValue

data class RenameRecordingState(
	val isRenaming: Boolean = false,
	val showDialog: Boolean = false,
	val textFieldState: TextFieldValue = TextFieldValue(),
	val isBlank: Boolean = false,
)
