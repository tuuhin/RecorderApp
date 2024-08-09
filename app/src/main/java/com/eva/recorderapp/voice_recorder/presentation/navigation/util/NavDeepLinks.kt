package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import androidx.core.net.toUri

object NavDeepLinks {
	private val baseUri = "app://com.eva.recorderapp"

	val recorderDestinationPattern = baseUri
	val recorderDestinationUri = recorderDestinationPattern.toUri()

	val recordingsDestinationPattern = baseUri + "/recordings"
	val recordingsDestinationUri = recordingsDestinationPattern.toUri()

	val appPlayerDestinationPattern = baseUri + "/player/{audioId}"
	fun audioPlayerDestinationUri(id: Long) = (baseUri + "/player" + "/${id}").toUri()
}