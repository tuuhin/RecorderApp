package com.eva.player.domain.model

/**
 * Represents the various playback states of a media player.
 */
enum class PlayerPlayState {
	/**
	 * The player is currently paused.
	 *
	 * In this state, playback is not progressing, and the player is waiting for a
	 * command to resume playing.
	 */
	PAUSED,

	/**
	 * The player is actively playing media content.
	 *
	 * In this state, the playback position is advancing, and the user can see or
	 * hear the content.
	 */
	PLAYING,

	/**
	 * The player is temporarily stopped to buffer content.
	 *  This state occurs when the player does not have enough data available for playback.
	 *  It will automatically transition back to [PLAYING] once a sufficient amount of content
	 *  is buffered.
	 */
	BUFFERING

}
