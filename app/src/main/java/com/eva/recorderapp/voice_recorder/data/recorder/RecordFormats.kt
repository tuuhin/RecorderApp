package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.OutputFormat
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.voice_recorder.domain.recorder.RecordEncoderAndFormat

object RecordFormats {

	val AMR = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_NB,
		outputFormat = OutputFormat.AMR_NB,
		mimeType = MimeTypes.AUDIO_AMR
	)

	val AMR_WB = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_WB,
		outputFormat = AudioEncoder.AMR_WB,
		mimeType = MimeTypes.AUDIO_AMR_WB
	)

	val M4A = RecordEncoderAndFormat(
		encoder = AudioEncoder.HE_AAC,
		outputFormat = OutputFormat.MPEG_4,
		mimeType = MimeTypes.AUDIO_MP4
	)

	val OGG = RecordEncoderAndFormat(
		encoder = AudioEncoder.OPUS,
		outputFormat = OutputFormat.OGG,
		mimeType = MimeTypes.AUDIO_OGG
	)

}




