package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PlayerSliderControl(private val player: AudioFilePlayer) {

	//seek amount we have certain amount for the player and user
	private val _seekAmountByUser = MutableStateFlow(0.seconds)

	// flag to control is its controlled by the user
	private val _seekControlledByUser = MutableStateFlow(false)

	// debounced controller flag
	@OptIn(FlowPreview::class)
	private val _isSeekPlayerUserControlled = _seekControlledByUser
		.debounce(80.milliseconds)

	val trackData = combine(
		player.playerMetaDataFlow,
		_seekAmountByUser, player.trackInfoAsFlow, _isSeekPlayerUserControlled
	) { metaData, userAmt, trackInfo, flag ->
		val state = metaData.playerState
		val useTrackData = !flag || state == PlayerState.PLAYING
		// every change will be delayed by 20 milliseconds
		delay(30.milliseconds)
		// set the current pos as user amount if user has selected or plyer is in ready state
		val current = if (useTrackData) trackInfo.current else userAmt
		trackInfo.copy(current = current)

	}.distinctUntilChanged()


	fun onSliderSlide(amount: Duration) {
		_seekControlledByUser.update { true }
		_seekAmountByUser.update { amount }
	}

	fun onSliderSlideComplete() {
		player.onSeekDuration(_seekAmountByUser.value)
		_seekControlledByUser.update { false }
	}
}