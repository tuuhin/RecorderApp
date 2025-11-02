package com.eva.player_shared.state

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
internal class PlayerSliderController {

	private val _mutex = MutatorMutex()

	private val _seekAmountByUser = MutableStateFlow(Duration.ZERO)
	val seekAmountByUser = _seekAmountByUser.asStateFlow()

	private val _isSeekByUser = MutableStateFlow(false)

	val isSeekByUser = _isSeekByUser.debounce { controlled ->
		if (controlled) 0.seconds
		else 75.milliseconds
	}.distinctUntilChanged()

	suspend fun onSliderSlide(amount: Duration) {
		_mutex.mutate(MutatePriority.UserInput) {
			_isSeekByUser.update { true }
			_seekAmountByUser.update { amount }
		}
	}

	suspend fun sliderCleanUp(onDone: suspend () -> Unit = {}) {
		_mutex.mutate(MutatePriority.PreventUserInput) {
			_isSeekByUser.update { false }
			onDone()
		}
	}
}