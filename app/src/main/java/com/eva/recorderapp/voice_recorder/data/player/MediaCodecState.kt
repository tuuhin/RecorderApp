package com.eva.recorderapp.voice_recorder.data.player

import android.media.MediaCodec

enum class MediaCodecState {
	/**
	 * [MediaCodec] stopped, its either started or configured
	 */
	STOP,

	/**
	 * [MediaCodec] state executing mediacodec is running
	 */
	EXEC,

	/**
	 * [MediaCodec] state released
	 */
	END
}

internal val MediaCodec.BufferInfo.isEof: Boolean
	get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
