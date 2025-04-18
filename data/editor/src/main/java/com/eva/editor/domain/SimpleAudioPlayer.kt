package com.eva.editor.domain

import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface SimpleAudioPlayer {

	val isPlaying: StateFlow<Boolean>

	val trackInfoAsFlow: Flow<PlayerTrackData>

	fun onSeekDuration(duration: Duration)

	suspend fun preparePlayer(audio: AudioFileModel)

	suspend fun trimMediaItem(start: Duration, end: Duration)

	suspend fun pausePlayer()

	suspend fun startOrResumePlayer()

	suspend fun stopPlayer()

	suspend fun cleanUp()
}