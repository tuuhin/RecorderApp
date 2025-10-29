package com.eva.player.domain

import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface AudioFilePlayer {

	/**
	 * Emits the current track's playback data, including the current position and total duration.
	 * This flow provides real-time updates as the media plays.
	 *
	 * @see PlayerTrackData
	 */
	val trackInfoAsFlow: Flow<PlayerTrackData>

	/**
	 * Emits the current playing state (`true` if playing, `false` if paused or stopped).
	 * This provides a simple boolean flag for consumers who only need to know if playback is active.
	 */
	val isPlaying: Flow<Boolean>

	/**
	 * Emits a comprehensive snapshot of the player's metadata, including its state,
	 * playback speed, and repeat/mute settings.
	 *
	 * @see PlayerMetaData
	 */
	val playerMetaDataFlow: Flow<PlayerMetaData>

	/**
	 * A flow that emits `true` when the underlying media controller is ready for use.
	 * For implementations that do not require an media controller, this should return an [Flow] false
	 * or a flow that immediately emits `true`.
	 */
	val isControllerReady: Flow<Boolean>

	/**
	 * Controls the  playback speed of the player
	 * @param playBackSpeed Playback speed for the current player
	 * @see PlayerPlayBackSpeed
	 */
	fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed)

	/**
	 * Set is the player is looping the current media
	 * @param loop indicating if loop is allowed or not
	 */
	fun setPlayLooping(loop: Boolean = false)

	/**
	 * Seek the player to a certain duration on the timeline
	 * @param duration Amount of [Duration] to be sought on the player
	 */
	fun onSeekDuration(duration: Duration)

	/**
	 * Prepares the controller for the player
	 * @param audioId ID used by the player session
	 */
	suspend fun prepareController(audioId: Long) {}

	/**
	 * Prepares the player from a [AudioFileModel]
	 * @param audio The audio model which will be used to play the recording
	 * @return [Result] indicating player was prepared correctly
	 */
	suspend fun preparePlayer(audio: AudioFileModel): Result<Boolean>

	/**
	 * Seeks the player by n [duration] forward or backward
	 * @param rewind If player is to be rewind set [rewind]
	 * @param duration The amount of duration to seek forward
	 */
	fun seekPlayerByNDuration(duration: Duration = 2.seconds, rewind: Boolean = false)

	/**
	 * Mutes the player although the player work the player volume is set to zero
	 */
	fun onMuteDevice()

	/**
	 * Pauses the ongoing play
	 */
	suspend fun pausePlayer()

	/**
	 * Starts or Resume the play, make sure the player is configured to use it to start the play
	 */
	suspend fun startOrResumePlayer()

	/**
	 * Stops the current player
	 */
	suspend fun stopPlayer()

	/**
	 * Remove callbacks and resources associated with the player
	 */
	fun cleanUp()
}