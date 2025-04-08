package com.eva.feature_recordings.rename.state

import androidx.compose.ui.text.input.TextFieldValue

internal sealed interface RenameRecordingEvent {

	data class OnTextValueChange(val textValue: TextFieldValue) : RenameRecordingEvent

	data object OnRenameRecording : RenameRecordingEvent

	data class OnWriteAccessChanged(val isAllowed: Boolean, val message: String = "") :
		RenameRecordingEvent

}