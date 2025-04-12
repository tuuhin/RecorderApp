package com.eva.utils

object NavDeepLinks {
	private const val BASE_URI = "app://com.eva.recorderapp"

	const val RECORDER_DESTINATION_PATTERN = BASE_URI
	const val RECORDING_DESTINATION_PATTERN = "$BASE_URI/recordings"

	const val PLAYER_DESTINATION_PATTERN = "$BASE_URI/player/{audioId}"


	fun audioPlayerDestinationUri(id: Long) = (BASE_URI + "/player" + "/${id}")
}