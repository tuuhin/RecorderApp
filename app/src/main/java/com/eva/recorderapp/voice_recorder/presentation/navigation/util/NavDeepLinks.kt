package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import androidx.core.net.toUri

object NavDeepLinks {
	private const val BASE_URI = "app://com.eva.recorderapp"

	const val recorderDestinationPattern = BASE_URI
	val recorderDestinationUri = recorderDestinationPattern.toUri()

	const val recordingsDestinationPattern = "$BASE_URI/recordings"
	val recordingsDestinationUri = recordingsDestinationPattern.toUri()

	const val appPlayerDestinationPattern = "$BASE_URI/player/{audioId}"


	fun audioPlayerDestinationUri(id: Long) = (BASE_URI + "/player" + "/${id}").toUri()
}