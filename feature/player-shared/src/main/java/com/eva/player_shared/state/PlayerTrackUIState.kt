package com.eva.player_shared.state

import android.util.Log
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "TrackUIState"

@OptIn(
	FlowPreview::class,
	ExperimentalCoroutinesApi::class
)
class PlayerTrackUIState {

	private val _mutex = MutatorMutex()

	private val _seekAmountByUser = MutableStateFlow(Duration.ZERO)
	private val _isSeekByUser = MutableStateFlow(false)

	fun controllablePlayerTrackData(originalTrackData: Flow<PlayerTrackData>): Flow<PlayerTrackData> {
		return _isSeekByUser
			.debounce { isSeeking -> if (isSeeking) 0.milliseconds else 75.milliseconds }
			.distinctUntilChanged()
			.flatMapLatest { isSeeking ->
				Log.d(TAG, "IS SEEKING $isSeeking")
				// if the user is not seeking the item original track data is given
				if (!isSeeking) return@flatMapLatest originalTrackData
				// now user is seeking so we provide the seek data
				combine(originalTrackData, _seekAmountByUser) { track, current ->
					track.copy(current = current)
				}
			}
	}

	suspend fun onSliderValueChange(seekAmount: Duration) {
		_mutex.mutate(MutatePriority.UserInput) {
			Log.d(TAG, "SLIDER POSITION CHANGE")
			_isSeekByUser.value = true
			_seekAmountByUser.value = seekAmount
		}
	}

	suspend fun onInteractionFinished(onSeekComplete: (Duration) -> Unit) {
		if (!_isSeekByUser.value) return
		Log.d(TAG, "SLIDER INTERACTION COMPLETED")
		// Complete the seek operation
		_mutex.mutate(MutatePriority.PreventUserInput) {
			onSeekComplete(_seekAmountByUser.value)
			_isSeekByUser.value = false
		}
	}
}