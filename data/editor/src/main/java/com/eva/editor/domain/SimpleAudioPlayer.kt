package com.eva.editor.domain

import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface SimpleAudioPlayer {

	val isPlaying: Flow<Boolean>

	val trackInfoAsFlow: Flow<PlayerTrackData>

	val isMediaItemChanged: Flow<Boolean>

	fun onSeekDuration(duration: Duration)

	suspend fun prepareAudioFile(audio: AudioFileModel)

	suspend fun cropMediaPortion(audio: AudioFileModel, config: AudioClipConfig)

	suspend fun cutMediaPortion(audio: AudioFileModel, config: AudioClipConfig)

	suspend fun pausePlayer()

	suspend fun startOrResumePlayer()

	suspend fun stopPlayer()

	fun cleanUp()
}