package com.eva.player.data.reader

import android.media.MediaCodec

internal enum class MediaCodecState {
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