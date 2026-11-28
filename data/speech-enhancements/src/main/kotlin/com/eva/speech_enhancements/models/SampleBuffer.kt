package com.eva.speech_enhancements.models

internal class SampleBuffer {

    private var _data = FloatArray(0)
    private var _head = 0
    private var _size = 0

    val size: Int
        get() = _size

    fun clear() {
        _head = 0
        _size = 0
    }

    fun append(values: FloatArray) {
        if (values.isEmpty()) return
        ensureCapacity(_head + _size + values.size)
        values.copyInto(_data, _head + _size)
        _size += values.size
    }

    fun dropFront(count: Int) {
        if (count <= 0) return
        require(count <= _size) { "cannot drop more samples than buffered" }
        _head += count
        _size -= count
        if (_head >= (1 shl 16).coerceAtLeast(_data.size / 4)) compact()
    }

    fun takeFront(count: Int): FloatArray {
        if (count <= 0) return FloatArray(0)
        require(count <= _size) { "cannot take more samples than buffered" }
        val out = _data.copyOfRange(_head, _head + count)
        dropFront(count)
        return out
    }

    private fun ensureCapacity(required: Int) {
        if (required <= _data.size) return
        compact()
        if (required <= _data.size) return
        _data = FloatArray(maxOf(required, _data.size * 2))
    }

    private fun compact() {
        if (_head == 0) return
        _data.copyInto(_data, 0, _head, _head + _size)
        _head = 0
    }
}
