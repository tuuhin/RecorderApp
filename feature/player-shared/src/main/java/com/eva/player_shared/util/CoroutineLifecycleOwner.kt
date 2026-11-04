package com.eva.player_shared.util

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job

internal class CoroutineLifecycleOwner(context: CoroutineContext) : LifecycleOwner {

    private val mainHandler = Handler(Looper.getMainLooper())

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.INITIALIZED
        }

    init {
        val job = context[Job]
        if (job?.isActive == true) {
            runOnMain { lifecycle.currentState = Lifecycle.State.RESUMED }
            job.invokeOnCompletion {
                runOnMain { lifecycle.currentState = Lifecycle.State.DESTROYED }
            }
        } else {
            runOnMain {
                lifecycle.currentState = Lifecycle.State.CREATED
                lifecycle.currentState = Lifecycle.State.DESTROYED
            }
        }
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post { block() }
    }
}
