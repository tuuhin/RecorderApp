package com.eva.datastore.domain.models

import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders

data class RecorderAudioSettings(
	val encoders: RecordingEncoders = RecordingEncoders.ACC,
	val quality: RecordQuality = RecordQuality.NORMAL,
	val pauseRecordingOnCall: Boolean = false,
	val skipSilences: Boolean = false,
	val enableStereo: Boolean = false,
	val useBluetoothMic: Boolean = false,
	val addLocationInfoInRecording: Boolean = false,
)
