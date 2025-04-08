package com.eva.datastore.data.mappers

import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.NamingFormatProto
import com.eva.datastore.proto.RecorderEncodingFormatsProto
import com.eva.datastore.proto.RecorderQualityProto
import com.eva.datastore.proto.RecorderSettingsProto

internal fun RecorderSettingsProto.toDomain(): RecorderAudioSettings = RecorderAudioSettings(
	encoders = encoder.toDomain,
	quality = quality.toDomain,
	pauseRecordingOnCall = pauseDuringCalls,
	enableStereo = isStereoMode,
	skipSilences = skipSilences,
	useBluetoothMic = useBluetoothMic,
	addLocationInfoInRecording = allowLocationInfoIfAvailable,
)

internal fun FileSettingsProto.toDomain(): RecorderFileSettings = RecorderFileSettings(
	name = prefix,
	format = format.toDomain,
	allowExternalRead = allowExternalRead
)

internal val RecorderQualityProto.toDomain: RecordQuality
	get() = when (this) {
		RecorderQualityProto.QUALITY_NORMAL -> RecordQuality.NORMAL
		RecorderQualityProto.QUALITY_HIGH -> RecordQuality.HIGH
		RecorderQualityProto.QUALITY_LOW -> RecordQuality.LOW
		RecorderQualityProto.UNRECOGNIZED -> RecordQuality.NORMAL
	}

internal val NamingFormatProto.toDomain: AudioFileNamingFormat
	get() = when (this) {
		NamingFormatProto.FORMAT_VIA_DATE -> AudioFileNamingFormat.DATE_TIME
		NamingFormatProto.FORMAT_VIA_COUNT -> AudioFileNamingFormat.COUNT
		NamingFormatProto.UNRECOGNIZED -> AudioFileNamingFormat.DATE_TIME
	}

internal val RecorderEncodingFormatsProto.toDomain: RecordingEncoders
	get() = when (this) {
		RecorderEncodingFormatsProto.ENCODER_AMR_NB -> RecordingEncoders.AMR_NB
		RecorderEncodingFormatsProto.ENCODER_AMR_WB -> RecordingEncoders.AMR_WB
		RecorderEncodingFormatsProto.ENCODER_ACC -> RecordingEncoders.ACC
		RecorderEncodingFormatsProto.ENCODER_OPTUS -> RecordingEncoders.OPTUS
		RecorderEncodingFormatsProto.UNRECOGNIZED -> RecordingEncoders.ACC
	}