package com.eva.recorderapp.voice_recorder.data.player

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.eva.recorderapp.R

object PlayerSessionCommands {

	const val REWIND_BY_1SEC = "REWIND_BY_10_SECONDS"
	const val FORWARD_BY_1SEC = "FORWARD_BY_10_SECONDS"

	val rewindButton = createCustomButton(
		action = REWIND_BY_1SEC,
		displayName = REWIND_BY_1SEC,
		icon = R.drawable.ic_fast_rewind
	)

	val forwardButton =
		createCustomButton(
			action = FORWARD_BY_1SEC,
			displayName = FORWARD_BY_1SEC,
			icon = R.drawable.ic_fast_forward
		)

	val buttonsAsList = listOf(rewindButton, forwardButton)

	private fun createCustomButton(
		action: String,
		displayName: String,
		@DrawableRes icon: Int,
		extras: Bundle = Bundle.EMPTY
	): CommandButton {
		val sessionCommand = SessionCommand(action, extras)
		return CommandButton.Builder()
			.setDisplayName(displayName)
			.setSessionCommand(sessionCommand)
			.setIconResId(icon)
			.setEnabled(true)
			.build()
	}

	fun playPauseButton(
		showPauseButton: Boolean,
		extras: Bundle = Bundle.EMPTY,
		displayName: String = ""
	): CommandButton {
		return CommandButton.Builder()
			.setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
			.setIconResId(
				if (showPauseButton) R.drawable.ic_pause
				else R.drawable.ic_play
			)
			.setExtras(extras)
			.setDisplayName(displayName)
			.build()
	}

}