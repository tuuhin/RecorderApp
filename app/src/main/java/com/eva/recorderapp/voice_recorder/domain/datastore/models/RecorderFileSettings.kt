package com.eva.recorderapp.voice_recorder.domain.datastore.models

data class RecorderFileSettings(
	val nameStyle: String = "Voice",
	val nameFormat: AudioFileNamingFormat = AudioFileNamingFormat.DATE_TIME,
)
