package com.eva.player.data.service

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.SessionCommand
import com.eva.player.R

internal object PlayerSessionCommands {

	const val REWIND_BY_1SEC = "REWIND_BY_10_SECONDS"
	const val FORWARD_BY_1SEC = "FORWARD_BY_10_SECONDS"

	val rewindButton: CommandButton
		get() = createCustomButton(REWIND_BY_1SEC, REWIND_BY_1SEC, R.drawable.ic_fast_rewind)

	val forwardButton: CommandButton
		get() = createCustomButton(FORWARD_BY_1SEC, FORWARD_BY_1SEC, R.drawable.ic_fast_forward)

	val buttonsAsList: List<CommandButton>
		get() = listOf(rewindButton, forwardButton)

	private fun createCustomButton(
		action: String,
		displayName: String,
		@DrawableRes icon: Int,
		extras: Bundle = Bundle.EMPTY,
	): CommandButton {
		val sessionCommand = SessionCommand(action, extras)
		return CommandButton.Builder(ICON_UNDEFINED)
			.setCustomIconResId(icon)
			.setDisplayName(displayName)
			.setCustomIconResId(icon)
			.setSessionCommand(sessionCommand)
			.setEnabled(true)
			.build()
	}

	fun playPauseButton(
		showPauseButton: Boolean,
		extras: Bundle = Bundle.EMPTY,
		displayName: String = "",
	): CommandButton {
		val resId = if (showPauseButton) R.drawable.ic_pause
		else R.drawable.ic_play

		return CommandButton.Builder(ICON_UNDEFINED)
			.setCustomIconResId(resId)
			.setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
			.setExtras(extras)
			.setDisplayName(displayName)
			.build()
	}
}