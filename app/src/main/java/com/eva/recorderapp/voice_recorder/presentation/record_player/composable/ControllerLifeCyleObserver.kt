package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ControllerEvents

@Composable
fun ControllerLifeCyleObserver(
	onEvent: (ControllerEvents) -> Unit,
) {

	val updatedOnEvent by rememberUpdatedState(newValue = onEvent)
	val lifeCycleOwner = LocalLifecycleOwner.current


	DisposableEffect(key1 = lifeCycleOwner) {

		val observer = LifecycleEventObserver { _, event ->

			if (event == Lifecycle.Event.ON_START) {
				updatedOnEvent(ControllerEvents.OnAddController)
			}
			if (event == Lifecycle.Event.ON_STOP) {
				updatedOnEvent(ControllerEvents.OnRemoveController)
			}
		}

		// adding observer
		lifeCycleOwner.lifecycle.addObserver(observer)

		onDispose {
			// remove observer
			lifeCycleOwner.lifecycle.removeObserver(observer)
		}
	}
}