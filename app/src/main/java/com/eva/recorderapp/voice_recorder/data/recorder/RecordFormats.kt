package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.OutputFormat
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.voice_recorder.domain.recorder.RecordEncoderAndFormat

object RecordFormats {

	val THREE_GPP = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_NB,
		outputFormat = OutputFormat.AMR_NB,
		mimeType = MimeTypes.AUDIO_AMR_NB
	)

	val MP3 = RecordEncoderAndFormat(
		encoder = AudioEncoder.DEFAULT,
		outputFormat = OutputFormat.MPEG_4,
		mimeType = MimeTypes.AUDIO_MP4
	)

	val WEBM = RecordEncoderAndFormat(
		encoder = AudioEncoder.VORBIS,
		outputFormat = OutputFormat.WEBM,
		mimeType = MimeTypes.AUDIO_WEBM
	)
}




