package com.eva.player.data.reader

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

internal val MediaFormat.pcmEncoding: Int
	get() = if (containsKey(MediaFormat.KEY_PCM_ENCODING)) {
		when (getInteger(MediaFormat.KEY_PCM_ENCODING)) {
			AudioFormat.ENCODING_PCM_8BIT -> 8
			AudioFormat.ENCODING_PCM_16BIT -> 16
			AudioFormat.ENCODING_PCM_32BIT -> 32
			else -> 16
		}
	} else 16

internal val MediaFormat.channels: Int
	get() = getInteger(MediaFormat.KEY_CHANNEL_COUNT)

internal val MediaFormat.sampleRate: Int
	get() = getInteger(MediaFormat.KEY_SAMPLE_RATE)

internal val MediaFormat.duration: Duration
	get() = getLong(MediaFormat.KEY_DURATION).microseconds

internal val MediaFormat.mimeType: String?
	get() = getString(MediaFormat.KEY_MIME)

internal val MediaCodec.BufferInfo.isEof: Boolean
	get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
