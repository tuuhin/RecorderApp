package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.compose.ui.text.input.TextFieldValue

data class RenameRecordingState(
	val isRenaming: Boolean = false,
	val textFieldState: TextFieldValue = TextFieldValue(),
	val errorString: String = "",
) {
	val hasError: Boolean
		get() = errorString.isNotBlank()
}
