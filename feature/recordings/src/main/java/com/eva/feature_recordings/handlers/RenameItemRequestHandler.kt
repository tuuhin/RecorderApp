package com.eva.feature_recordings.handlers

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.eva.feature_recordings.rename.state.RenamePermissionEvent
import com.eva.feature_recordings.rename.state.RenameRecordingEvent
import com.eva.recordings.data.wrapper.RecordingsMediaRequester
import com.eva.ui.R
import kotlinx.coroutines.flow.Flow

@Composable
internal fun RenameItemRequestHandler(
	onWriteAccessChange: (RenameRecordingEvent) -> Unit,
	permissionEventDeferred: () -> Flow<RenamePermissionEvent>,
) {
	val lifeCycleOwner = LocalLifecycleOwner.current
	val context = LocalContext.current
	val currentOnWriteAccessChange by rememberUpdatedState(onWriteAccessChange)

	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->
			val isAccepted = result.resultCode == Activity.RESULT_OK

			val message = if (isAccepted) context.getString(R.string.write_request_accepted)
			else context.getString(R.string.write_request_rejected)

			val event = RenameRecordingEvent.OnWriteAccessChanged(isAccepted, message)
			currentOnWriteAccessChange(event)
		}
	)


	LaunchedEffect(key1 = lifeCycleOwner) {
		lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			permissionEventDeferred().collect { event ->
				when (event) {
					is RenamePermissionEvent.OnAskAccessRequest -> {
						// intent with the intent request
						val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
							RecordingsMediaRequester.createWriteRequest(context, event.recordings)
						// intent with the Recordings provider request
						else event.intentSenderRequest

						// launch the permission request
						request?.let(permissionLauncher::launch)
					}
				}
			}
		}
	}
}