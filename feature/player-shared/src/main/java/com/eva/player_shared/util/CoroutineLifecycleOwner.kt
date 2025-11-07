package com.eva.player_shared.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

internal class CoroutineLifecycleOwner(context: CoroutineContext) : LifecycleOwner {

	private val lifecycleRegistry = LifecycleRegistry(this)
		.apply { currentState = Lifecycle.State.INITIALIZED }

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry

	init {
		if (context[Job]?.isActive == true) {
			lifecycleRegistry.currentState = Lifecycle.State.RESUMED
			context[Job]?.invokeOnCompletion {
				lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
			}
		} else {
			lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
		}
	}
}