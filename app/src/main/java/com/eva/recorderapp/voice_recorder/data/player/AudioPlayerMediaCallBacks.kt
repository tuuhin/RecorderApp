package com.eva.recorderapp.voice_recorder.data.player

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.eva.recorderapp.common.IntentRequestCodes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture


@OptIn(UnstableApi::class)
class AudioPlayerMediaCallBacks(
	private val context: Context
) : MediaSession.Callback {

	override fun onConnect(
		session: MediaSession,
		controller: MediaSession.ControllerInfo
	): MediaSession.ConnectionResult {

		val commands = PlayerSessionCommands.buttonsAsList
			.mapNotNull(CommandButton::sessionCommand)

		val acceptedResult = AcceptedResultBuilder(session)
			.setAvailablePlayerCommands(
				ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
					.remove(Player.COMMAND_SEEK_TO_NEXT)
					.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
					.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
					.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
					.build()
			).setAvailableSessionCommands(
				ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
					.addSessionCommands(commands)
					.build()
			)
			.build()

		return acceptedResult
	}

	override fun onPostConnect(
		session: MediaSession,
		controller: MediaSession.ControllerInfo
	) {

		val layout = ImmutableList.of(
			PlayerSessionCommands.rewindButton,
			PlayerSessionCommands.forwardButton
		)
		val audioId = session.player.mediaMetadata
			.extras?.getLong("AUDIO_FILE_ID", -1) ?: -1

		val playerIntent = if (audioId != -1L)
			createPlayerIntent(audioId) else null

		playerIntent?.let { intent ->
			session.setSessionActivity(controller, intent)
		}
		if (controller.controllerVersion != 0) {
			session.setCustomLayout(layout)
		}
	}

	override fun onCustomCommand(
		session: MediaSession,
		controller: MediaSession.ControllerInfo,
		customCommand: SessionCommand,
		args: Bundle
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

	private fun createPlayerIntent(audioId: Long): PendingIntent? =
		context.packageManager.getLaunchIntentForPackage(context.packageName)
			?.apply {
				data = NavDeepLinks.audioPlayerDestinationUri(audioId)
			}?.let { intent ->
				PendingIntent.getActivity(
					context,
					IntentRequestCodes.PLAYER_NOTIFICATION_INTENT.code,
					intent,
					PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
				)
			}

}