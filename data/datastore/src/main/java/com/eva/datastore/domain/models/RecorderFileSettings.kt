package com.eva.datastore.domain.models

import com.eva.datastore.domain.enums.AudioFileNamingFormat


data class RecorderFileSettings(
	val name: String = "Voice",
	val format: AudioFileNamingFormat = AudioFileNamingFormat.DATE_TIME,
	val allowExternalRead: Boolean = false,
)
