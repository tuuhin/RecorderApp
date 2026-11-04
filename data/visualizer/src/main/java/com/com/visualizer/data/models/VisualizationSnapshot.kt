package com.com.visualizer.data.models

import com.com.visualizer.domain.VisualizerSession
import com.com.visualizer.domain.VisualizerState

internal data class VisualizationSnapshot(
    val owner: VisualizerSession? = null,
    val state: VisualizerState = VisualizerState.NOT_STARTED,
    val values: FloatArray = floatArrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisualizationSnapshot

        if (owner != other.owner) return false
        if (state != other.state) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
