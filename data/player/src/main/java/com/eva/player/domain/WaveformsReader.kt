package com.eva.player.domain

import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias RMSValues = List<Float>

interface WaveformsReader {

	val wavefront: Flow<RMSValues>

	val isReaderRunning: Flow<Boolean>

	suspend fun performWaveformsReading(audioId: Long): Resource<Unit, Exception>

	fun clearResources()
}