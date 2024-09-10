package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.DialogUIEvent
import com.eva.recorderapp.common.DialogViewModel
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider

@Composable
fun <T : AppViewModel> UiEventsSideEffect(viewModel: T) {

	val context = LocalContext.current
	val lifecyleOwner = LocalLifecycleOwner.current
	val snackBarState = LocalSnackBarProvider.current

	LaunchedEffect(key1 = lifecyleOwner) {
		lifecyleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.uiEvent.collect { event ->
				when (event) {
					is UIEvents.ShowToast -> {
						Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
							.show()
					}

					is UIEvents.ShowSnackBarWithActions -> {
						val result = snackBarState.showSnackbar(
							message = event.message,
							actionLabel = event.actionText,
							withDismissAction = event.actionText != null,
							duration = if (event.long) SnackbarDuration.Long else SnackbarDuration.Short
						)
						when (result) {
							SnackbarResult.ActionPerformed -> event.action()
							else -> {}
						}
					}

					is UIEvents.ShowSnackBar -> snackBarState.showSnackbar(message = event.message)
				}

			}
		}
	}
}

@Composable
fun <T : DialogViewModel> DialogSideEffectHandler(viewModel: T, navController: NavController) {

	val context = LocalContext.current
	val lifecyleOwner = LocalLifecycleOwner.current

	LaunchedEffect(key1 = lifecyleOwner) {
		lifecyleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.uiEvent.collect { event ->
				when (event) {
					DialogUIEvent.CloseDialog -> navController.popBackStack()
					is DialogUIEvent.ShowToast -> {
						Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
							.show()
					}
				}
			}
		}
	}
}