package com.com.visualizer.utils

import android.os.Handler
import android.os.Looper

fun Handler?.safePOST(runnable: Runnable): Boolean {
	if (this == null) return false
	if (!looper.thread.isAlive) return false
	return post(runnable)
}

fun Handler?.isThreadAlive(ignoreMainThread: Boolean = true): Boolean {
	val isAlive = this?.looper?.thread?.isAlive ?: false
	val isMainLooper = this?.looper == Looper.getMainLooper()
    return !(isMainLooper && ignoreMainThread) && isAlive
}
