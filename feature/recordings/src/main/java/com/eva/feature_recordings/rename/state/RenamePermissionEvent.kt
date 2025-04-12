package com.eva.feature_recordings.rename.state

import androidx.activity.result.IntentSenderRequest
import com.eva.recordings.domain.models.RecordedVoiceModel

internal sealed interface RenamePermissionEvent {

	data class OnAskAccessRequest(
		val recordings: RecordedVoiceModel,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : RenamePermissionEvent
}