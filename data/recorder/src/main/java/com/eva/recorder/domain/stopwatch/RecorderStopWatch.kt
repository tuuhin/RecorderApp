@file:OptIn(ExperimentalTime::class)

package com.eva.recorder.domain.stopwatch

import com.eva.recorder.domain.models.RecorderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class RecorderStopWatch(
	private val delayTime: Duration = 80.milliseconds,
) {

	private val scope = CoroutineScope(Dispatchers.Default)

	private val _state = MutableStateFlow(RecorderState.IDLE)
	val recorderState = _state.asStateFlow()

	private val _elapsedTime = MutableStateFlow(0)

	@OptIn(ExperimentalCoroutinesApi::class)
	val elapsedTime = _elapsedTime
		.mapLatest { current -> LocalTime.fromMillisecondOfDay(current) }
		.stateIn(
			scope = scope,
			started = SharingStarted.Companion.WhileSubscribed(2000),
			initialValue = LocalTime(0, 0, 0)
		)

	init {
		updateElapsedTime()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private fun updateElapsedTime() = _state
		.flatMapLatest { state ->
			runStopWatch(isRunning = state == RecorderState.RECORDING)
		}
		.onEach { add -> _elapsedTime.update { prev -> prev + add } }
		.launchIn(scope)


	private fun runStopWatch(isRunning: Boolean): Flow<Int> = flow {
		var previous = Clock.System.now()
		while (isRunning) {
			val now = Clock.System.now()
			if (now > previous) {
				val diff = now.minus(previous)
				val diffInMillis = diff.toInt(DurationUnit.MILLISECONDS)
				emit(diffInMillis)
			}
			previous = Clock.System.now()
			delay(delayTime)
		}
	}.flowOn(Dispatchers.Default)


	fun startOrResume() = _state.update { RecorderState.RECORDING }

	fun pause() = _state.update { RecorderState.PAUSED }

	fun prepare() = _state.update { RecorderState.PREPARING }

	fun stop() {
		// completes the timer and reset the elapsed time
		_state.update { RecorderState.COMPLETED }
		_elapsedTime.update { 0 }
	}

	fun cancel() {
		// cancel the current run
		_state.update { RecorderState.CANCELLED }
		_elapsedTime.update { 0 }
	}

	fun reset() {
		//cancels the scope
		scope.cancel()
		// update the state
		_state.update { RecorderState.IDLE }
	}

}