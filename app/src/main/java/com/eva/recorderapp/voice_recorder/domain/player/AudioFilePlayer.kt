package com.eva.recorderapp.voice_recorder.domain.player

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface AudioFilePlayer {


	val trackInfoAsFlow: Flow<PlayerTrackData>
	val playerMetaDataFlow: Flow<PlayerMetaData>

	fun onMuteDevice()


	fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed)

	fun setPlayLooping(loop: Boolean = false)

	fun onSeekDuration(duration: Duration)

	/**
	 * Prepares the player from a [AudioFileModel]
	 * @param audio The audio model which will be used to play the recording
	 * @return [Resource.Success] indicating everything went well otherwise [Resource.Error]
	 */
	suspend fun preparePlayer(audio: AudioFileModel): Resource<Boolean, Exception>

	/**
	 * Seeks the player by n [duration] forward or backward
	 * @param rewind If player is to be rewind set [rewind] to [true]
	 * @param duration The amount of duration to seek forward
	 */
	fun seekPlayerByNDuration(duration: Duration = 2.seconds, rewind: Boolean = false)


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


	fun addPlayerListener()

	/**
	 * Remove callbacks and resouces associated with the player
	 */
	fun clearPlayerListener()
}