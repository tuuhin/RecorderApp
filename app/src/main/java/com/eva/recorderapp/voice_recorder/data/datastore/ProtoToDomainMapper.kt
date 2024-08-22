package com.eva.recorderapp.voice_recorder.data.datastore

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings

fun RecorderSettingsProto.toDomain(): RecorderAudioSettings = RecorderAudioSettings(
	encoders = encoder.toDomain,
	quality = quality.toDomain,
	blockCallsDuringRecording = blockIncommingCalls,
)

fun FileSettingsProto.toDomain(): RecorderFileSettings = RecorderFileSettings(
	name = namingStyle,
	format = format.toDomain
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

val RecorderEncodingFormatsProto.toDomain: RecordingEncoders
	get() = when (this) {
		RecorderEncodingFormatsProto.ENCODER_ACC -> RecordingEncoders.ACC
		RecorderEncodingFormatsProto.UNRECOGNIZED -> RecordingEncoders.ACC
		RecorderEncodingFormatsProto.ENCODER_AMR_NB -> RecordingEncoders.AMR_NB
		RecorderEncodingFormatsProto.ENCODER_AMR_WB -> RecordingEncoders.AMR_WB
		RecorderEncodingFormatsProto.ENCODER_MPEG -> RecordingEncoders.MPEG
	}