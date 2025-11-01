package com.eva.feature_recordings.handlers

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.eva.feature_recordings.recordings.state.RecordingScreenEvent
import com.eva.feature_recordings.util.DeleteOrTrashRequestEvent
import com.eva.recordings.data.wrapper.RecordingsMediaRequester
import com.eva.ui.R
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance

@Composable
internal fun TrashItemRequestHandler(
	eventsFlow: () -> SharedFlow<DeleteOrTrashRequestEvent>,
	onResult: (RecordingScreenEvent) -> Unit,
) {

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val currentOnResults by rememberUpdatedState(onResult)

	val trashRequestLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->

			val message = if (result.resultCode == Activity.RESULT_OK)
				context.getString(R.string.recording_delete_request_success)
			else context.getString(R.string.recording_delete_request_failed)

			val event = RecordingScreenEvent.OnPostTrashRequest(message)
			currentOnResults(event)
		}
	)

	val rememberedFlow = remember(eventsFlow) { eventsFlow() }

	// handle trash request for Api 30+
	LaunchedEffect(key1 = lifecycleOwner, key2 = rememberedFlow) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			rememberedFlow
				.filterIsInstance<DeleteOrTrashRequestEvent.OnTrashRequest>()
				.collect { event ->
					if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
						val request = event.intentSenderRequest ?: return@collect
						trashRequestLauncher.launch(request)
					} else RecordingsMediaRequester.createTrashRequest(context, event.recordings)
						?.let(trashRequestLauncher::launch)
				}
		}
	}
}