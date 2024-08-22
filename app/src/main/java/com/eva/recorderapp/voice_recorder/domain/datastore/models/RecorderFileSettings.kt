package com.eva.recorderapp.voice_recorder.domain.datastore.models

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat

data class RecorderFileSettings(
	val name: String = "Voice",
	val format: AudioFileNamingFormat = AudioFileNamingFormat.DATE_TIME,
)
