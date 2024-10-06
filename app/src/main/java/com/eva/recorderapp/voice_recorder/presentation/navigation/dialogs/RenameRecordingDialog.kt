package com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenameDialogViewModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenamePermissionEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenameRecordingEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenameRecordingsDialogContent

fun NavGraphBuilder.renameRecordingDialog(
	controller: NavController,
) = dialog<NavDialogs.RenameRecordingDialog>(
	dialogProperties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
) {

	val lifeCycleOwner = LocalLifecycleOwner.current
	val context = LocalContext.current

	val viewModel = hiltViewModel<RenameDialogViewModel>()
	val renameState by viewModel.renameState.collectAsStateWithLifecycle()

	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->
			val isAccepted = result.resultCode == Activity.RESULT_OK

			val message = if (isAccepted) context.getString(R.string.write_request_accepted)
			else context.getString(R.string.write_request_rejected)

			val event = RenameRecordingEvent.OnWriteAccessChanged(isAccepted, message)
			viewModel.onEvent(event)
		}
	)
	

	LaunchedEffect(key1 = lifeCycleOwner) {
		lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.permissionEvent.collect { event ->
				when (event) {
					is RenamePermissionEvent.OnAskAccessRequest -> {
						// intent with the intent request
						val request = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
							event.intentSenderRequest
						// intent with the Recordings provider request
						else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
							RecordingsProvider.createWriteRequest(context, event.recordings)
						// for other case nothing
						else null

						// launch the permission request
						request?.let(permissionLauncher::launch)
					}
				}
			}
		}
	}

	UiEventsSideEffect(
		eventsFlow = viewModel::uiEvent,
		onPopScreenEvent = dropUnlessResumed(block = controller::popBackStack)
	)

	RenameRecordingsDialogContent(
		state = renameState,
		onEvent = viewModel::onEvent,
		onDismissRequest = dropUnlessResumed(block = controller::popBackStack),
	)
}