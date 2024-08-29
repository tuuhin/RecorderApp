package com.eva.recorderapp.voice_recorder.data.datastore

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders

val RecordQuality.toProto: RecorderQualityProto
	get() = when (this) {
		RecordQuality.HIGH -> RecorderQualityProto.QUALITY_HIGH
		RecordQuality.NORMAL -> RecorderQualityProto.QUALITY_NORMAL
		RecordQuality.LOW -> RecorderQualityProto.QUALITY_LOW
	}

val AudioFileNamingFormat.toProto: NamingFormatProto
	get() = when (this) {
		AudioFileNamingFormat.DATE_TIME -> NamingFormatProto.FORMAT_VIA_DATE
		AudioFileNamingFormat.COUNT -> NamingFormatProto.FORMAT_VIA_COUNT
	}

val RecordingEncoders.toProto: RecorderEncodingFormatsProto
	get() = when (this) {
		RecordingEncoders.AMR_NB -> RecorderEncodingFormatsProto.ENCODER_AMR_NB
		RecordingEncoders.AMR_WB -> RecorderEncodingFormatsProto.ENCODER_AMR_WB
		RecordingEncoders.ACC -> RecorderEncodingFormatsProto.ENCODER_ACC
		RecordingEncoders.OPTUS -> RecorderEncodingFormatsProto.ENCODER_OPTUS
	}