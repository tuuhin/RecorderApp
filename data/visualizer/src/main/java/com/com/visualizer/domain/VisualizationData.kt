package com.com.visualizer.domain

data class VisualizationData(
    val state: VisualizerState = VisualizerState.NOT_STARTED,
    val values: FloatArray = floatArrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisualizationData

        if (state != other.state) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
