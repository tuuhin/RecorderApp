package com.eva.recorderapp.voice_recorder.data.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.DefaultMediaNotificationProvider.NotificationIdProvider
import androidx.media3.session.MediaSession
import com.eva.recorderapp.R
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.data.player.PlayerSessionCommands
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
class AudioPlayerNotification(
	private val context: Context
) : DefaultMediaNotificationProvider(
	context,
	NotificationIdProvider { NotificationConstants.PLAYER_NOTIFICATION_ID },
	NotificationConstants.PLAYER_CHANNEL_ID,
	R.string.player_channel_resource_id
) {
	override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence?
		= metadata.displayTitle ?: metadata.title

	override fun getMediaButtons(
		session: MediaSession,
		playerCommands: Player.Commands,
		customLayout: ImmutableList<CommandButton>,
		showPauseButton: Boolean
	): ImmutableList<CommandButton> {
		super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
		val builder = ImmutableList.Builder<CommandButton>()

		if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
			builder.add(
				PlayerSessionCommands.playPauseButton(
					showPauseButton = showPauseButton,
					extras = bundleOf(COMMAND_KEY_COMPACT_VIEW_INDEX to C.INDEX_UNSET),
					displayName = if (showPauseButton) "Paused" else "Play"
				)
			)
		}

		customLayout.filter { it.sessionCommand != null }
			.forEach(builder::add)

		return builder.build()
	}

}