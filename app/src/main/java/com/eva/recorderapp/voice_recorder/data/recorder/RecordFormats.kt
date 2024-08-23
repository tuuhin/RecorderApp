package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.OutputFormat
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders
import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat

object RecordFormats {

	val THREE_GPP = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_NB,
		outputFormat = OutputFormat.AMR_NB,
		mimeType = MimeTypes.AUDIO_AMR_NB
	)

	val AMR_WB = RecordEncoderAndFormat(
		encoder = AudioEncoder.AMR_WB,
		outputFormat = AudioEncoder.AMR_WB,
		mimeType = MimeTypes.AUDIO_AMR_WB
	)

	// audio/mp4 -> audio/mp3
	val MP3 = RecordEncoderAndFormat(
		encoder = AudioEncoder.AAC,
		outputFormat = OutputFormat.AAC_ADTS,
		mimeType = MimeTypes.AUDIO_MP4
	)

	// audio/mpeg -> audio/m4a
	val M4A = RecordEncoderAndFormat(
		encoder = AudioEncoder.HE_AAC,
		outputFormat = OutputFormat.MPEG_4,
		mimeType = MimeTypes.AUDIO_MPEG
	)

}

val RecordingEncoders.recordFormat: RecordEncoderAndFormat
	get() = when (this) {
		RecordingEncoders.MP3 -> RecordFormats.MP3
		RecordingEncoders.THREE_GPP -> RecordFormats.THREE_GPP
		RecordingEncoders.AMR_WB -> RecordFormats.AMR_WB
		RecordingEncoders.MP4 -> RecordFormats.M4A
	}



