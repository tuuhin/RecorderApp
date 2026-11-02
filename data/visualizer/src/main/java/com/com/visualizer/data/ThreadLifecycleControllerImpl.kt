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
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.measureTime

private const val TAG = "THREAD_CONTROLLER"

@OptIn(ExperimentalAtomicApi::class)
internal class ThreadLifecycleControllerImpl(private val threadName: String) :
	DefaultLifecycleObserver, ThreadController {

	@Volatile
	private var _handlerThread: HandlerThread? = null

	@Volatile
	private var _handler: Handler? = null

	private val _exceptionHandler = Thread.UncaughtExceptionHandler { thread, exc ->
		Log.e(TAG, "THREADING ERRORS NAME:${thread.name} STATE:${thread.state}", exc)
	}

	private val _isStopping = AtomicBoolean(false)

	override fun onDestroy(owner: LifecycleOwner) {
		try {
			val requested = _isStopping.load()
			if (requested) return
			if (_handler == null) return
			Log.d(TAG, "STOP THREAD ON END_OF_LIFECYCLE")
			stopThreadInternal(600L)
		} finally {
			_isStopping.store(false)
			owner.lifecycle.addObserver(this@ThreadLifecycleControllerImpl)
			Log.d(TAG, "THREAD OBSERVER REMOVED!")
		}
	}

	@MainThread
	@Synchronized
	override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler {
		lifecycleOwner.lifecycle.addObserver(this@ThreadLifecycleControllerImpl)
		Log.d(TAG, "NEW THREAD OBSERVER ADDED!!")
		return getHandler()
	}

	override fun stopThread(handler: Handler?, maxWaitTime: Long) {
		if (_handler?.looper?.thread?.thId != handler?.looper?.thread?.thId) {
			Log.w(TAG, "INCORRECT HANDLER PROVIDED CANNOT STOP CURRENT")
			Log.w(TAG, "CURRENT THREAD THREAD :${Thread.currentThread()}")
			return
		}
		try {
			val requested = _isStopping.load()
			if (requested) return
			if (_handler == null) return
			Log.d(TAG, "STOP THREAD VIA CALL")
			stopThreadInternal(maxWaitTime)
		} finally {
			_isStopping.store(false)
		}
	}

	private fun getHandler(): Handler {
		if (_handlerThread == null || _handlerThread?.isAlive == false) createThread()
		return _handler!!
	}

	@Suppress("DEPRECATION")
	@Synchronized
	private fun createThread() {
		if (_handlerThread?.isAlive == true) {
			Log.w(TAG, "THREAD IS NOT KILLED")
			return
		}

		val newThread = HandlerThread(threadName, Process.THREAD_PRIORITY_AUDIO).apply {
            uncaughtExceptionHandler = _exceptionHandler
			start()
		}
		// set the new handler
		_handlerThread = newThread
		_handler = Handler.createAsync(newThread.looper)

		val message = buildString {
			append("HANDLER THREAD IS SET: ")
			append("NAME: ${newThread.name} |")
			append("STATE: ${newThread.looper.thread.state} |")
			append("ID: ${newThread.thId} |")
			append("PRIORITY :${newThread.priority}")
		}
		Log.i(TAG, message)
	}

	/**
	 * Stop [_handlerThread] from running anymore
	 * @param maxWaitTime Time in millis the thread should wait for thread to die
	 */
	@Synchronized
	private fun stopThreadInternal(maxWaitTime: Long) {
		require(maxWaitTime > 0) { "Wait time need to be greater than 0" }

		val handlerThread = _handlerThread ?: run {
			Log.w(TAG, "HANDLER THREAD WAS NOT SET OR ALREADY HANDLED")
			return
		}
		val handler = _handler ?: return
		handler.removeCallbacksAndMessages(null)
		Log.i(TAG, "STOPPING THREAD THREAD_ID:${handler.looper.thread.thId}")
		try {
            val safeRequest = handlerThread.isAlive && handlerThread.quitSafely()

			if (!safeRequest) {
				Log.d(TAG, "LOOPER WAS NOT SET OF THE THREAD IS ALREADY KILLED")
				return
			}
			Log.i(TAG, "THREAD STATE BEFORE JOIN: ${handlerThread.state}")
			// blocking code
			val duration = measureTime { handlerThread.join(maxWaitTime) }
			Log.d(TAG, "JOIN TOOK :$duration")
			Log.i(TAG, "THREAD STATE AFTER JOIN: ${handlerThread.state}")

		} catch (e: InterruptedException) {
			Log.e(TAG, "THREAD JOIN FAILED", e)
			e.printStackTrace()
		} finally {
			Log.i(TAG, "CLEANING UP DONE!")
			Log.d(TAG, "AFTER CLEAN UP STATE: ${_handlerThread?.state}")
			_handlerThread?.uncaughtExceptionHandler = null
			_handlerThread = null
			_handler = null
		}
	}

	@Suppress("DEPRECATION")
	private val Thread.thId: Long
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) threadId() else id
}
