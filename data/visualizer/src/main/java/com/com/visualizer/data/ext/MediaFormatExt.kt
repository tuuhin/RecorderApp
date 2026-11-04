package com.com.visualizer.data.ext

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

internal val MediaFormat.pcmEncoding: PcmSampleEncoding
	get() = if (containsKey(MediaFormat.KEY_PCM_ENCODING)) {
		when (getInteger(MediaFormat.KEY_PCM_ENCODING)) {
            AudioFormat.ENCODING_PCM_8BIT -> PcmSampleEncoding.UNSIGNED_8
            AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_PCM_16BIT -> PcmSampleEncoding.SIGNED_16
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> PcmSampleEncoding.SIGNED_24
            AudioFormat.ENCODING_PCM_32BIT -> PcmSampleEncoding.SIGNED_32
            AudioFormat.ENCODING_PCM_FLOAT -> PcmSampleEncoding.FLOAT_32
            else -> throw IllegalArgumentException("Unsupported PCM encoding: ${getInteger(MediaFormat.KEY_PCM_ENCODING)}")
		}
    } else PcmSampleEncoding.SIGNED_16

internal val MediaFormat.channels: Int
	get() = if (containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
		getInteger(MediaFormat.KEY_CHANNEL_COUNT)
	} else 1

internal val MediaFormat.sampleRate: Int
	get() = getInteger(MediaFormat.KEY_SAMPLE_RATE)

internal val MediaFormat.duration: Duration?
	get() = if (containsKey(MediaFormat.KEY_DURATION))
		getLong(MediaFormat.KEY_DURATION).microseconds
	else null

internal val MediaFormat.mimeType: String?
	get() = if (containsKey(MediaFormat.KEY_MIME))
		getString(MediaFormat.KEY_MIME)
	else null

internal val MediaCodec.BufferInfo.isEndOfStream: Boolean
	get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
