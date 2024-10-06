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
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.TrashRecordingScreenEvent
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun DeleteRecordingRequestHandler(
	eventsFlow: () -> SharedFlow<DeleteOrTrashRecordingsRequest>,
	onResult: (TrashRecordingScreenEvent) -> Unit,
) {

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val deleteRequestEvents by rememberUpdatedState(eventsFlow)

	val deleteRequestLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->
			val message =
				if (result.resultCode == Activity.RESULT_OK)
					context.getString(R.string.recording_delete_request_success)
				else context.getString(R.string.recording_delete_request_failed)

			val event = TrashRecordingScreenEvent.OnPostDeleteRequest(message)
			onResult(event)
		},
	)

	LaunchedEffect(lifecycleOwner) {

		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

			val flow = deleteRequestEvents()

			flow.collect { event ->
				when (event) {
					is DeleteOrTrashRecordingsRequest.OnDeleteRequest -> {
						if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
							event.intentSenderRequest?.let { request ->
								deleteRequestLauncher.launch(request)
							}
						} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
							RecordingsProvider.createDeleteRequest(context, event.trashRecordings)
								?.let(deleteRequestLauncher::launch)
						}
					}

					else -> {}
				}
			}
		}
	}
}