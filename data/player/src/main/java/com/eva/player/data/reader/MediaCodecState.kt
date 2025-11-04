package com.eva.player.data.reader

import android.media.MediaCodec

internal enum class MediaCodecState {

	/**
	 * [MediaCodec] is not processing anything with configured or stopped or uninitialized
	 */
	STOPPED,

	/**
	 * [MediaCodec] is currently reading/writing the  output/input buffers
	 */
	EXEC,

	/**
	 * [MediaCodec] is not required any more
	 */
	RELEASED
}