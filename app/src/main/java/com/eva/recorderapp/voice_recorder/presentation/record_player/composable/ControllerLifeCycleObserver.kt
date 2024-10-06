package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.eventFlow
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ControllerEvents

@Composable
fun ControllerLifeCycleObserver(
	audioId: Long,
	onEvent: (ControllerEvents) -> Unit,
) {
	val lifeCycleOwner = LocalLifecycleOwner.current
	val updatedOnEvent by rememberUpdatedState(newValue = onEvent)

	LaunchedEffect(key1 = lifeCycleOwner, key2 = audioId) {
		lifeCycleOwner.lifecycle.eventFlow.collect { event ->

			if (event == Lifecycle.Event.ON_START) {
				updatedOnEvent(ControllerEvents.OnAddController(audioId))
			}

			if (event == Lifecycle.Event.ON_STOP) {
				updatedOnEvent(ControllerEvents.OnRemoveController)
			}
		}
	}
}