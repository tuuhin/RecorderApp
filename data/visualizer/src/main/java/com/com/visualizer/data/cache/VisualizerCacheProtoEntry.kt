package com.com.visualizer.data.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class VisualizerCacheProtoEntry(
    @ProtoNumber(1) val hash: String,
    @ProtoNumber(2) val timePerPointMs: Int,
    @ProtoNumber(3) val amplitudeScale: Float,
    @ProtoNumber(4) val pointCount: Int,
    @ProtoNumber(5) val points: ByteArray,
)
