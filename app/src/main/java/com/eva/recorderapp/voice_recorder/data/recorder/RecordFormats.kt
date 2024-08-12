package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaFormat
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.OutputFormat
import com.eva.recorderapp.voice_recorder.domain.recorder.RecordEncoderAndFormat

object RecordFormats {

	val AMR = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_NB,
		outputFormat = OutputFormat.AMR_NB,
		mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_NB
	)

	val AMR_WB = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_WB,
		outputFormat = AudioEncoder.AMR_WB,
		mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_WB
	)

	val M4A = RecordEncoderAndFormat(
		encoder = AudioEncoder.AAC,
		outputFormat = OutputFormat.MPEG_4,
		mimeType = MediaFormat.MIMETYPE_AUDIO_MPEG
	)

	val OGG = RecordEncoderAndFormat(
		encoder = AudioEncoder.OPUS,
		outputFormat = OutputFormat.OGG,
		mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS
	)

}




