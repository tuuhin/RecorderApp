package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.milliseconds

class RecorderStopWatch {

	private val scope = CoroutineScope(Dispatchers.Default)

	private val _state = MutableStateFlow(RecorderState.IDLE)
	val recorderState = _state.asStateFlow()

	private val _elapsedTime = MutableStateFlow(0L)
	val elapsedTime = _elapsedTime
		.map { current -> LocalTime.fromNanosecondOfDay(current) }
		.stateIn(
			scope = scope,
			started = SharingStarted.WhileSubscribed(2000),
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


	private fun runStopWatch(isRunning: Boolean): Flow<Long> = flow {
		var previous = Clock.System.now()
		while (isRunning) {
			val now = Clock.System.now()
			if (now > previous) {
				val diff = now.minus(previous)
				val diffNano = diff.inWholeNanoseconds
				emit(diffNano)
			}
			previous = Clock.System.now()
			delay(50.milliseconds)
		}
	}.flowOn(Dispatchers.Default)


	fun startOrResume() = _state.update { RecorderState.RECORDING }

	fun pause() = _state.update { RecorderState.PAUSED }

	fun stop() {
		// completes the timer and reset the elpased time
		_state.update { RecorderState.COMPLETED }
		_elapsedTime.update { 0L }
	}

	fun reset() {
		//cancels the scope
		scope.cancel()
		// update the state
		_state.update { RecorderState.IDLE }
	}

}