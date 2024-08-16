package com.eva.recorderapp.voice_recorder.domain.datastore.models

data class RecorderAudioSettings(
	val encoders: RecordingEncoders = RecordingEncoders.ACC,
	val quality: RecordQuality = RecordQuality.NORMAL,
	val blockCallsDuringRecording: Boolean = false,
	val useBluetoothHeadSet: Boolean = false,
	val skipSilences: Boolean = false,
	val enableStero: Boolean = false,
)
