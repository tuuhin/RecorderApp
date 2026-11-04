package com.com.visualizer.domain

import kotlin.uuid.Uuid

data class VisualizerSession(
    val audioId: Long,
    val computeToken: String = Uuid.random().toHexString(),
)
