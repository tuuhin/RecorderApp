package com.eva.recorderapp.voice_recorder.presentation.recordings.handlers

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

	val trashRequestLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->

			val message = if (result.resultCode == Activity.RESULT_OK)
				context.getString(R.string.recording_delete_request_success)
			else context.getString(R.string.recording_delete_request_failed)

			val event = RecordingScreenEvent.OnPostTrashRequest(message)
			onResult(event)
		}
	)

	val rememberedFlow = remember(eventsFlow) { eventsFlow() }

	// handle trash request for Api 30+
	LaunchedEffect(key1 = lifecycleOwner, key2 = rememberedFlow) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			rememberedFlow.collect { event ->
				when (event) {
					is DeleteOrTrashRecordingsRequest.OnTrashRequest -> {
						if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
							val request = event.intentSenderRequest ?: return@collect
							trashRequestLauncher.launch(request)

						} else RecordingsProvider.createTrashRequest(context, event.recordings)
							?.let(trashRequestLauncher::launch)
					}

					else -> {}
				}
			}
		}
	}
}