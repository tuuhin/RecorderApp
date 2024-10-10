package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ControllerEvents

@Composable
fun ControllerLifeCycleObserver(
	audioId: Long,
	onEvent: (ControllerEvents) -> Unit,
) {
	val lifeCycleOwner = LocalLifecycleOwner.current
	val updatedEvent by rememberUpdatedState(onEvent)

	LifecycleResumeEffect(key1 = audioId, lifecycleOwner = lifeCycleOwner) {
		// on resume
		updatedEvent(ControllerEvents.OnAddController)

		// on pause
		onPauseOrDispose {
			updatedEvent(ControllerEvents.OnRemoveController)
		}
	}
}