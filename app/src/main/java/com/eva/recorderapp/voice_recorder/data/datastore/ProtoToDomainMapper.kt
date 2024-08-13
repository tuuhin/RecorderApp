package com.eva.recorderapp.voice_recorder.data.datastore

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderNameFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordingEncoders

fun RecorderSettingsProto.toDomain() = RecorderSettings(
	nameFormat = format.toDomain,
	encoders = encoder.toDomain,
	quality = quality.toDomain,
	blockCallsDuringRecording = blockIncommingCalls,
	useBluetoothHeadSet = useBluetoothMic
)

val RecorderQualityProto.toDomain: RecordQuality
	get() = when (this) {
		RecorderQualityProto.QUALITY_NORMAL -> RecordQuality.NORMAL
		RecorderQualityProto.QUALITY_HIGH -> RecordQuality.HIGH
		RecorderQualityProto.QUALITY_LOW -> RecordQuality.LOW
		RecorderQualityProto.UNRECOGNIZED -> RecordQuality.NORMAL
	}

val RecoderFormatProto.toDomain: RecorderNameFormat
	get() = when (this) {
		RecoderFormatProto.FORMAAT_VIA_DATE -> RecorderNameFormat.DATE_TIME
		RecoderFormatProto.FORMAT_VIA_COUNT -> RecorderNameFormat.COUNT
		RecoderFormatProto.UNRECOGNIZED -> RecorderNameFormat.DATE_TIME
	}

val RecoderEncoderProto.toDomain
	get() = when (this) {
		RecoderEncoderProto.ENCODER_MP3 -> RecordingEncoders.ACC
		RecoderEncoderProto.ENCODER_ACC -> RecordingEncoders.ACC
		RecoderEncoderProto.UNRECOGNIZED -> RecordingEncoders.ACC
	}