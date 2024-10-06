package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.activity.result.IntentSenderRequest
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel

sealed interface RenamePermissionEvent {

	data class OnAskAccessRequest(
		val recordings:RecordedVoiceModel,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : RenamePermissionEvent
}