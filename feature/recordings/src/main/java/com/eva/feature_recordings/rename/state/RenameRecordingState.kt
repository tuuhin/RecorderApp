package com.eva.feature_recordings.rename.state

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.recordings.domain.models.RecordedVoiceModel

internal data class RenameRecordingState(
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
