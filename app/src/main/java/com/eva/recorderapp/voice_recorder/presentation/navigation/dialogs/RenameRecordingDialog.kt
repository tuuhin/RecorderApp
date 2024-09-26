package com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenameDialogViewModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog.RenameRecordingsDialogContent

fun NavGraphBuilder.renameRecordingDialog(
	controller: NavController,
) = dialog<NavDialogs.RenameRecordingDialog>(
	dialogProperties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
) {

	val viewModel = hiltViewModel<RenameDialogViewModel>()
	val renameState by viewModel.renameState.collectAsStateWithLifecycle()

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