package com.com.visualizer.data.models

import android.os.Handler
import androidx.media3.common.util.UnstableApi
import com.com.visualizer.data.MediaCodecPCMDataDecoder
import com.com.visualizer.domain.VisualizerSession

@UnstableApi
internal class ActiveRequest(
    val generation: Long,
    val playerId: Long,
    val fileUri: String,
    @Volatile var session: VisualizerSession?,
    val cacheKey: String,
    var hash: String? = null,
    var handler: Handler? = null,
    var decoder: MediaCodecPCMDataDecoder? = null,
    var timePerPointInMs: Int,
    var requestedPointMs: Int,
)
