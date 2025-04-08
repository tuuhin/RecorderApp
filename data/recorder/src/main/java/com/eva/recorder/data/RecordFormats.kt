package com.eva.recorder.data

import android.media.MediaRecorder
import android.webkit.MimeTypeMap
import androidx.media3.common.MimeTypes
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.recorder.domain.models.RecordEncoderAndFormat

private val mimeTypesMap = MimeTypeMap.getSingleton()

internal object RecordFormats {

	val THREE_GPP = RecordEncoderAndFormat(
		encoder = MediaRecorder.AudioEncoder.AMR_NB,
		outputFormat = MediaRecorder.OutputFormat.AMR_NB,
		mimeType = MimeTypes.AUDIO_AMR,
	)

	val AMR_WB = RecordEncoderAndFormat(
		encoder = MediaRecorder.AudioEncoder.AMR_WB,
		outputFormat = MediaRecorder.OutputFormat.AMR_WB,
		mimeType = MimeTypes.AUDIO_AMR_WB,
	)


	val M4A = RecordEncoderAndFormat(
		encoder = MediaRecorder.AudioEncoder.AAC,
		outputFormat = MediaRecorder.OutputFormat.MPEG_4,
		mimeType = MimeTypes.AUDIO_MP4,
	)

	val OGG = RecordEncoderAndFormat(
		encoder = MediaRecorder.AudioEncoder.OPUS,
		outputFormat = MediaRecorder.OutputFormat.OGG,
		mimeType = MimeTypes.AUDIO_OGG,
	)

}

internal val RecordEncoderAndFormat.fileExtension: String?
	get() = mimeTypesMap.getExtensionFromMimeType(mimeType)
		?.let { ext -> ".$ext" }

internal val RecordingEncoders.recordFormat: RecordEncoderAndFormat
	get() = when (this) {
		RecordingEncoders.AMR_NB -> RecordFormats.THREE_GPP
		RecordingEncoders.AMR_WB -> RecordFormats.AMR_WB
		RecordingEncoders.ACC -> RecordFormats.M4A
		RecordingEncoders.OPTUS -> RecordFormats.OGG
	}



