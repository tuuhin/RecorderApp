package com.eva.recorderapp.voice_recorder.domain.datastore.models

data class RecorderSettings(
	val encoders: RecordingEncoders = RecordingEncoders.ACC,
	val quality: RecordQuality = RecordQuality.NORMAL,
	val nameFormat: RecorderNameFormat = RecorderNameFormat.DATE_TIME,
	val blockCallsDuringRecording: Boolean = false,
	val useBluetoothHeadSet: Boolean = false,
)
