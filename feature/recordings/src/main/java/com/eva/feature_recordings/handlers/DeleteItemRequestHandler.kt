package com.eva.feature_recordings.handlers

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
import com.eva.feature_recordings.bin.state.TrashRecordingScreenEvent
import com.eva.feature_recordings.util.DeleteOrTrashRequestEvent
import com.eva.recordings.data.wrapper.RecordingsMediaRequester
import com.eva.ui.R
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun DeleteItemRequestHandler(
	eventsFlow: () -> SharedFlow<DeleteOrTrashRequestEvent>,
	onResult: (TrashRecordingScreenEvent) -> Unit,
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

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

	val rememberedFlow = remember(eventsFlow) { eventsFlow() }

	LaunchedEffect(key1 = lifecycleOwner, key2 = eventsFlow) {

		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			rememberedFlow.filterIsInstance<DeleteOrTrashRequestEvent.OnDeleteRequest>()
				.collect { event ->
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						RecordingsMediaRequester.createDeleteRequest(context, event.trashRecordings)
							?.let(deleteRequestLauncher::launch)
					} else event.intentSenderRequest?.let(deleteRequestLauncher::launch)
				}
		}
	}
}