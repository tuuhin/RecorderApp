package com.eva.datastore.data.mappers

import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.proto.NamingFormatProto
import com.eva.datastore.proto.RecorderEncodingFormatsProto
import com.eva.datastore.proto.RecorderQualityProto

internal val RecordQuality.toProto: RecorderQualityProto
	get() = when (this) {
		RecordQuality.HIGH -> RecorderQualityProto.QUALITY_HIGH
		RecordQuality.NORMAL -> RecorderQualityProto.QUALITY_NORMAL
		RecordQuality.LOW -> RecorderQualityProto.QUALITY_LOW
	}

internal val AudioFileNamingFormat.toProto: NamingFormatProto
	get() = when (this) {
		AudioFileNamingFormat.DATE_TIME -> NamingFormatProto.FORMAT_VIA_DATE
		AudioFileNamingFormat.COUNT -> NamingFormatProto.FORMAT_VIA_COUNT
	}

internal val RecordingEncoders.toProto: RecorderEncodingFormatsProto
	get() = when (this) {
		RecordingEncoders.AMR_NB -> RecorderEncodingFormatsProto.ENCODER_AMR_NB
		RecordingEncoders.AMR_WB -> RecorderEncodingFormatsProto.ENCODER_AMR_WB
		RecordingEncoders.ACC -> RecorderEncodingFormatsProto.ENCODER_ACC
		RecordingEncoders.OPTUS -> RecorderEncodingFormatsProto.ENCODER_OPTUS
	}