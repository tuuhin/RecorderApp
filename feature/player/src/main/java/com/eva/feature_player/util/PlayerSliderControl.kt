package com.eva.feature_player.util

import com.eva.player.data.MediaControllerProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PlayerSliderControl(private val controller: MediaControllerProvider) {

	//seek amount we have certain amount for the player and user
	private val _seekAmountByUser = MutableStateFlow(0.seconds)

	// flag to control is it's controlled by the user
	private val _seekControlledByUser = MutableStateFlow(false)

	// debounced controller flag don't change the debounce amount
	@OptIn(FlowPreview::class)
	private val _isSeekPlayerUserControlled = _seekControlledByUser
		.debounce(110.milliseconds)

	val trackData = combine(
		_seekAmountByUser,
		controller.trackInfoAsFlow,
		_isSeekPlayerUserControlled
	) { userAmt, trackInfo, isControlled ->
		// set the current pos as user amount if user has selected or player is in ready state
		if (isControlled) trackInfo.copy(current = userAmt)
		else trackInfo
	}.distinctUntilChanged()


	/**
	 * On slider value change ongoing
	 */
	fun onSliderSlide(amount: Duration) {
		_seekControlledByUser.update { true }
		_seekAmountByUser.update { amount }
	}

	/**
	 * Slider Value change completed
	 */
	fun onSliderSlideComplete() {
		controller.player?.onSeekDuration(_seekAmountByUser.value)
		_seekControlledByUser.update { false }
	}
}