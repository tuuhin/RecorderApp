package com.com.visualizer.domain

import android.os.Handler
import androidx.lifecycle.LifecycleOwner

fun interface ThreadController {

	fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler?

}