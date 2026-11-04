package com.com.visualizer.data.ext

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
