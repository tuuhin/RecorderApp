package com.com.visualizer.data

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.com.visualizer.domain.ThreadController
import kotlin.time.measureTime

private const val TAG = "THREAD_CONTROLLER"

internal class ThreadLifecycleControllerImpl(private val threadName: String) :
	DefaultLifecycleObserver, ThreadController {

	@Volatile
	private var _handlerThread: HandlerThread? = null

	@Volatile
	private var _handler: Handler? = null

	private val _exceptionHandler = Thread.UncaughtExceptionHandler { thread, exc ->
		Log.e(TAG, "THREADING ERRORS NAME:${thread.name} STATE:${thread.state}", exc)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		owner.lifecycle.removeObserver(this)
		stopThread()
	}

	@MainThread
	@Synchronized
	override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler {
		lifecycleOwner.lifecycle.addObserver(this@ThreadLifecycleControllerImpl)
		return getHandler()
	}

	private fun getHandler(): Handler {
		if (_handlerThread == null || _handlerThread?.isAlive == false) createThread()
		return _handler!!
	}

	/**
	 * Prepares the handler for use
	 */
	@Suppress("DEPRECATION")
	@Synchronized
	private fun createThread() {
		if (_handlerThread?.isAlive == true) {
			Log.w(TAG, "THREAD IS NOT KILLED")
			return
		}

		val newThread = HandlerThread(threadName, Process.THREAD_PRIORITY_AUDIO).apply {
			setUncaughtExceptionHandler(_exceptionHandler)
			start()
		}
		// set the new handler
		_handlerThread = newThread
		_handler = Handler.createAsync(newThread.looper)

		val threadId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
			newThread.threadId() else newThread.id

		val message = buildString {
			append("HANDLER THREAD IS SET: ")
			append("NAME: ${newThread.name} |")
			append("STATE: ${newThread.looper.thread.state} |")
			append("ID: $threadId |")
			append("PRIORITY :${newThread.priority}")
		}
		Log.i(TAG, message)
	}

	/**
	 * Stop [_handlerThread] from running anymore
	 * @param maxWaitTime Time in millis the thread should wait for thread to die
	 */
	@Synchronized
	private fun stopThread(maxWaitTime: Long = 2_000L) {
		require(maxWaitTime > 0) { "Wait time need to be greater than 0" }

		val handlerThread = _handlerThread ?: run {
			Log.d(TAG, "HANDLER THREAD WAS NOT SET")
			return
		}
		val handler = _handler ?: return
		handler.removeCallbacksAndMessages(null)

		try {
			val safeRequest = if (handlerThread.isAlive)
				handlerThread.quitSafely() else false

			if (!safeRequest) {
				Log.d(TAG, "LOOPER WAS NOT SET OF THE THREAD IS ALREADY KILLED")
				return
			}
			Log.i(TAG, "THREAD QUIT, THREAD STATE: ${handlerThread.state}")
			// blocking code
			val duration = measureTime { handlerThread.join(maxWaitTime) }
			Log.d(TAG, "THREAD CURRENT STATE: ${handlerThread.state}")
			Log.d(TAG, "JOIN TOOK :$duration")

		} catch (e: InterruptedException) {
			Log.e(TAG, "THREAD JOIN FAILED", e)
			e.printStackTrace()
		} finally {
			Log.v(TAG, "AFTER CLEAN UP")
			Log.v(TAG, "STATE: ${_handlerThread?.state}")
			_handlerThread?.uncaughtExceptionHandler = null
			_handlerThread = null
			_handler = null
			//
		}
	}
}