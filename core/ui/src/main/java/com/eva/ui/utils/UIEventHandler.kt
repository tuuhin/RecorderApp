package com.eva.ui.utils

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.eva.ui.viewmodel.UIEvents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer

@Composable
fun UiEventsHandler(
	eventsFlow: () -> Flow<UIEvents>,
	onNavigateBack: () -> Unit = {},
) {

	val context = LocalContext.current
	val lifecyleOwner = LocalLifecycleOwner.current
	val snackBarState = LocalSnackBarProvider.current

	val updatedOnNavigateBack by rememberUpdatedState(newValue = onNavigateBack)

	LaunchedEffect(key1 = lifecyleOwner) {
		lifecyleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			// buffering it to show the last 4 events if extra items
			// are being processed drop old items
			// collect is too slow and collect latest is too fast
			eventsFlow().buffer(4, BufferOverflow.DROP_OLDEST).collect { event ->
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

					is UIEvents.ShowSnackBar -> snackBarState.showSnackbar(
						message = event.message,
						duration = SnackbarDuration.Short
					)

					UIEvents.PopScreen -> updatedOnNavigateBack()
				}
			}
		}
	}
}

