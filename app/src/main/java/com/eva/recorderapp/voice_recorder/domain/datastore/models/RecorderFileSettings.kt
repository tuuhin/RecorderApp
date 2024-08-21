package com.eva.recorderapp.voice_recorder.domain.datastore.models

data class RecorderFileSettings(
	val name: String = "Voice",
	val format: AudioFileNamingFormat = AudioFileNamingFormat.DATE_TIME,
)
