package com.eva.visualizer.data

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

class TestLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle = object : Lifecycle() {
        override fun addObserver(observer: LifecycleObserver) = Unit
        override fun removeObserver(observer: LifecycleObserver) = Unit
        override val currentState: State
            get() = State.RESUMED
    }
}
