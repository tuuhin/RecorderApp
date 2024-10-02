package com.eva.recorderapp.voice_recorder.domain.datastore.models

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders

data class RecorderAudioSettings(
	val encoders: RecordingEncoders = RecordingEncoders.ACC,
	val quality: RecordQuality = RecordQuality.NORMAL,
	val pauseRecordingOnCall: Boolean = false,
	val skipSilences: Boolean = false,
	val enableStereo: Boolean = false,
	val useBluetoothMic: Boolean = false,
	val addLocationInfoInRecording: Boolean = false,
)
