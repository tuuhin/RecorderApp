package com.eva.recorderapp.voice_recorder.data.datastore

import com.eva.recorderapp.voice_recorder.domain.datastore.models.AudioFileNamingFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordingEncoders

fun RecorderSettingsProto.toDomain(): RecorderAudioSettings = RecorderAudioSettings(
	encoders = encoder.toDomain,
	quality = quality.toDomain,
	blockCallsDuringRecording = blockIncommingCalls,
	useBluetoothHeadSet = useBluetoothMic
)

fun FileSettingsProto.toDomain(): RecorderFileSettings = RecorderFileSettings(
	nameStyle = namingStyle,
	nameFormat = format.toDomain
)

val RecorderQualityProto.toDomain: RecordQuality
	get() = when (this) {
		RecorderQualityProto.QUALITY_NORMAL -> RecordQuality.NORMAL
		RecorderQualityProto.QUALITY_HIGH -> RecordQuality.HIGH
		RecorderQualityProto.QUALITY_LOW -> RecordQuality.LOW
		RecorderQualityProto.UNRECOGNIZED -> RecordQuality.NORMAL
	}

val NamingFormatProto.toDomain: AudioFileNamingFormat
	get() = when (this) {
		NamingFormatProto.FORMAAT_VIA_DATE -> AudioFileNamingFormat.DATE_TIME
		NamingFormatProto.FORMAT_VIA_COUNT -> AudioFileNamingFormat.COUNT
		NamingFormatProto.UNRECOGNIZED -> AudioFileNamingFormat.DATE_TIME
	}

val RecoderEncoderProto.toDomain
	get() = when (this) {
		RecoderEncoderProto.ENCODER_MP3 -> RecordingEncoders.ACC
		RecoderEncoderProto.ENCODER_ACC -> RecordingEncoders.ACC
		RecoderEncoderProto.UNRECOGNIZED -> RecordingEncoders.ACC
	}