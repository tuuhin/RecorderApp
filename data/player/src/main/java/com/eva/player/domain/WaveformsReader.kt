package com.eva.player.domain

import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias RMSValues = List<Float>

interface WaveformsReader {

	val wavefront: Flow<RMSValues>

	val isReaderRunning: Flow<Boolean>

	suspend fun readWaveformsFromFile(audio: AudioFileModel): Resource<Unit, Exception>

	fun clearResources()
}