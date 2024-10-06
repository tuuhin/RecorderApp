package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel

data class RenameRecordingState(
	val isRenameAllowed: Boolean = false,
	val textFieldState: TextFieldValue = TextFieldValue(),
	val errorString: String = "",
	val recording: RecordedVoiceModel? = null,
) {
	val hasError: Boolean
		get() = errorString.isNotBlank()

	val canRename: Boolean
		get() = recording != null && isRenameAllowed
}
