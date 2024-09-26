package com.eva.recorderapp.voice_recorder.presentation.recordings.util.handlers

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
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsProvider
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RecordingScreenEvent
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun TrashRecordingsRequestHandler(
	eventsFlow: () -> SharedFlow<DeleteOrTrashRecordingsRequest>,
	onResult: (RecordingScreenEvent) -> Unit,
) {

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val trashEventsFlow by rememberUpdatedState(newValue = eventsFlow)

	val trashRequestLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->

			val message = if (result.resultCode == Activity.RESULT_OK)
				context.getString(R.string.recording_delete_request_success)
			else context.getString(R.string.recording_delete_request_failed)

			val isSuccess = result.resultCode == Activity.RESULT_OK
			val event = RecordingScreenEvent.OnPostTrashRequestApi30(isSuccess, message)

			onResult(event)
		}
	)

	// handle trash request for Api 30+
	LaunchedEffect(key1 = lifecycleOwner) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			val flow = trashEventsFlow()

			flow.collect { event ->
				when (event) {
					is DeleteOrTrashRecordingsRequest.OnTrashRequest -> {

						if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
							val request = event.intentSenderRequest ?: return@collect
							trashRequestLauncher.launch(request)

						} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
							val request = RecordingsProvider
								.createTrashRequest(context, event.recordings)
							trashRequestLauncher.launch(request)
						}
					}

					else -> {}
				}
			}
		}
	}
}