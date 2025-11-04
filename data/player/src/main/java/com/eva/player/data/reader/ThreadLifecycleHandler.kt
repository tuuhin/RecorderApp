package com.eva.player.data.reader

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.measureTime

private const val TAG = "THREAD_CONTROLLER"

internal class ThreadLifecycleHandler(private val threadName: String) : LifecycleEventObserver {

	@Volatile
	private var _handlerThread: HandlerThread? = null

	@Volatile
	private var _handler: Handler? = null

	private val _exceptionHandler = Thread.UncaughtExceptionHandler { thread, exc ->
		Log.e(TAG, "THREADING ERRORS NAME:${thread.name} STATE:${thread.state}", exc)
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (event == Lifecycle.Event.ON_CREATE) {
			Log.d(TAG, "PREPARING THREAD ON CREATE")
			createThread()
		} else if (event == Lifecycle.Event.ON_DESTROY) {
			Log.d(TAG, "STOPPING THREAD ON DESTROY")
			stopThread()
			source.lifecycle.removeObserver(this)
		}
	}

	suspend fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler {
		withContext(Dispatchers.Main) {
			lifecycleOwner.lifecycle.addObserver(this@ThreadLifecycleHandler)
		}
		return readHandler()
	}

	@Synchronized
	private fun readHandler(): Handler {
		if (_handlerThread == null || _handlerThread?.isAlive == false) createThread()
		return _handler!!
	}

	/**
	 * Prepares the handler for use
	 */
	@Suppress("DEPRECATION")
	@Synchronized
	private fun createThread() {
		val newThread = HandlerThread(threadName, Process.THREAD_PRIORITY_AUDIO).apply {
			setUncaughtExceptionHandler(_exceptionHandler)
			start()
		}
		val message = buildString {
			append("HANDLER THREAD IS SET: ")
			append("NAME: ${newThread.name} ")
			append("STATE: ${newThread.looper.thread.state} ")
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
				append("ID: ${newThread.threadId()} ")
			else append("ID: ${newThread.id} ")

			append("PRIORITY :${newThread.priority}")
		}
		Log.i(TAG, message)
		_handlerThread = newThread
		_handler = Handler(newThread.looper)
	}

	/**
	 * Stop [_handlerThread] from running anymore
	 * @param waitTime Time in millis the thread should wait for thread to die
	 */
	@Synchronized
	private fun stopThread(waitTime: Long = 800L) {
		val handlerThread = _handlerThread ?: run {
			Log.d(TAG, "HANDLER THREAD WAS NOT SET")
			return
		}
		val handler = _handler ?: return
		try {
			handler.removeCallbacksAndMessages(null)

			val safeRequest = if (handlerThread.isAlive) handlerThread.quitSafely() else false
			if (!safeRequest) {
				Log.d(TAG, "LOOPER IS NOT SET! OR THREAD IS DEAD")
				return
			}
			Log.i(TAG, "HANDLER THREAD QUIT SAFELY, THREAD STATE: ${handlerThread.state}")

			val duration = measureTime {
				if (handlerThread.isAlive) handlerThread.join(waitTime)
			}
			Log.i(TAG, "WAITED :$duration FOR COMPLETION")

			if (!handlerThread.isAlive) {
				Log.d(TAG, "THREAD IS DEAD CURRENT STATE: ${handlerThread.state}")
				return
			}

			Log.w(TAG, "REQUESTING TO FORCE QUIT")
			val requested2 = if (handlerThread.isAlive) handlerThread.quit() else false
			if (!requested2) {
				Log.i(TAG, "THREAD IS NOT RUNNING STATE IS_ALIVE:${handlerThread.isAlive}")
				return
			}

			Log.w(TAG, "THREAD IS ACTIVE NEED TO WAIT")
			val duration2 = measureTime {
				if (handlerThread.isAlive) handlerThread.join(waitTime * 2)
			}
			Log.w(TAG, "AGAIN WAITED FOR :$duration2")

			// thread killed
			if (!handlerThread.isAlive) {
				Log.d(TAG, "THREAD SHOULD BE KILLED BY NOW")
				return
			}

			Log.w(TAG, "THREAD IS STILL ALIVE AFTER QUIT SENDING INTERRUPT")
			if (handlerThread.isAlive) handlerThread.interrupt()
			if (handlerThread.isAlive) handlerThread.join(200)

		} catch (e: InterruptedException) {
			Log.e(TAG, "THREAD JOIN FAILED", e)
		} finally {
			Log.i(TAG, "AFTER CLEANUP :${_handlerThread?.state} ALIVE: ${_handlerThread?.isAlive}")
			_handlerThread?.uncaughtExceptionHandler = null
			_handlerThread = null
			_handler = null
		}
	}
}