package com.eva.player.data.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
internal class AudioPlayerMediaCallBacks : MediaSession.Callback {

	override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo)
			: MediaSession.ConnectionResult {

		val commands = PlayerSessionCommands.buttonsAsList
			.mapNotNull(CommandButton::sessionCommand)

		val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
			.buildUpon()
			.remove(Player.COMMAND_SEEK_TO_NEXT)
			.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
			.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
			.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
			.build()

		val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
			.buildUpon()
			.addSessionCommands(commands)
			.build()

		val result = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
			.setAvailablePlayerCommands(playerCommands)
			.setAvailableSessionCommands(sessionCommands)
			.build()

		return result
	}

	override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
		val layout = ImmutableList.of(
			PlayerSessionCommands.rewindButton,
			PlayerSessionCommands.forwardButton
		)

		if (controller.controllerVersion != 0) {
			session.setCustomLayout(layout)
		}
	}

	override fun onCustomCommand(
		session: MediaSession,
		controller: MediaSession.ControllerInfo,
		customCommand: SessionCommand,
		args: Bundle,
	): ListenableFuture<SessionResult> {
		when (customCommand.customAction) {
			PlayerSessionCommands.FORWARD_BY_1SEC -> {
				val newPos = session.player.currentPosition + 1000
				session.player.seekTo(newPos)
			}

			PlayerSessionCommands.REWIND_BY_1SEC -> {
				val newPos = session.player.currentPosition - 1000
				session.player.seekTo(newPos)
			}
		}
		return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
	}

}