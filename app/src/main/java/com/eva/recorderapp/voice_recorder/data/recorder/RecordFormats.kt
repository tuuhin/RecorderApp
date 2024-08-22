package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaFormat
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.OutputFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders
import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat

object RecordFormats {

	val ACC = RecordEncoderAndFormat(
		encoder = AudioEncoder.AAC,
		outputFormat = OutputFormat.AAC_ADTS,
		mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
	)

	val AMR_NB = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_NB,
		outputFormat = OutputFormat.AMR_NB,
		mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_NB
	)

	val AMR_WB = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_WB,
		outputFormat = AudioEncoder.AMR_WB,
		mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_WB
	)

	val MPEG = RecordEncoderAndFormat(
		encoder = AudioEncoder.AAC,
		outputFormat = OutputFormat.MPEG_4,
		mimeType = MediaFormat.MIMETYPE_AUDIO_MPEG
	)

}

val RecordingEncoders.recordFormat: RecordEncoderAndFormat
	get() = when (this) {
		RecordingEncoders.ACC -> RecordFormats.ACC
		RecordingEncoders.AMR_NB -> RecordFormats.AMR_NB
		RecordingEncoders.AMR_WB -> RecordFormats.AMR_WB
		RecordingEncoders.MPEG -> RecordFormats.MPEG
	}



