package com.com.visualizer.data

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.com.visualizer.domain.ThreadController

private const val TAG = "THREAD_CONTROLLER"

internal class ThreadLifecycleControllerImpl(private val threadName: String) :
    DefaultLifecycleObserver, ThreadController {

    @Volatile
    private var _handlerThread: HandlerThread? = null

    @Volatile
    private var _handler: Handler? = null

    // references to the lifecycle owner
    @Volatile
    private var _lifecycleOwner: LifecycleOwner? = null

    private val _exceptionHandler = Thread.UncaughtExceptionHandler { thread, exc ->
        Log.e(TAG, "THREADING ERRORS NAME:${thread.name} STATE:${thread.state}", exc)
    }

    @Synchronized
    override fun onDestroy(owner: LifecycleOwner) {
        // ensures we are using the correct  lifecycle owner
        if (_lifecycleOwner !== owner) return
        Log.d(TAG, "STOP THREAD ON END_OF_LIFECYCLE")
        stopThreadInternal(600L)
    }

    @MainThread
    @Synchronized
    override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler? {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return null
        // if the owner is not the referred owner detach the owner and set the new owner
        if (_lifecycleOwner !== lifecycleOwner) {
            detachOwner(_lifecycleOwner)
            _lifecycleOwner = lifecycleOwner
        }
        lifecycleOwner.lifecycle.addObserver(this@ThreadLifecycleControllerImpl)
        Log.d(TAG, "NEW THREAD OBSERVER ADDED!!")
        return getHandler()
    }

    @Synchronized
    override fun stopThread(handler: Handler?, maxWaitTime: Long) {
        if (handler == null || _handler !== handler) {
            Log.w(TAG, "INCORRECT HANDLER PROVIDED CANNOT STOP CURRENT")
            return
        }
        Log.d(TAG, "STOP THREAD VIA CALL")
        stopThreadInternal(maxWaitTime)
    }

    private fun getHandler(): Handler {
        if (_handlerThread == null || _handlerThread?.isAlive == false) {
            Log.i(TAG, "CREATING NEW THREAD")
            createThread()
        }
        return checkNotNull(_handler) { "Unable to start the visualizer thread" }
    }

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
            append("HANDLER INFO : ")
            append("NAME: ${newThread.name} |")
            append("STATE: ${newThread.looper.thread.state} |")
            append("ID: ${newThread.thId} |")
            append("PRIORITY :${newThread.priority}")
        }
        Log.i(TAG, "HANDLER THREAD IS READY")
        Log.i(TAG, message)
    }

    /**
     * Stop [_handlerThread] from running anymore
     * @param maxWaitTime Time in millis the thread should wait for thread to die
     */
    @Synchronized
    private fun stopThreadInternal(maxWaitTime: Long) {
        require(maxWaitTime > 0) { "Wait time must be positive" }

        val owner = _lifecycleOwner
        _lifecycleOwner = null
        // remove the owner
        detachOwner(owner)

        val handlerThread = _handlerThread ?: run {
            Log.w(TAG, "HANDLER THREAD WAS NOT SET OR ALREADY HANDLED")
            return
        }
        val handler = _handler ?: run {
            // clear the handler thread
            _handlerThread = null
            return
        }
        if (!handlerThread.isAlive) {
            // if handler thread is not alive clear everything
            handlerThread.uncaughtExceptionHandler = null
            _handlerThread = null
            _handler = null
            return
        }
        Log.i(TAG, "STOPPING THREAD THREAD_ID:${handler.looper.thread.thId}")
        try {
            val safeRequest = handlerThread.isAlive && handlerThread.quitSafely()

            if (!safeRequest) {
                Log.d(TAG, "LOOPER WAS NOT SET OF THE THREAD IS ALREADY KILLED")
                return
            }
            Log.i(TAG, "THREAD STATE BEFORE JOIN: ${handlerThread.state}")
            if (Thread.currentThread() !== handlerThread) handlerThread.join(maxWaitTime)
            Log.i(TAG, "THREAD STATE AFTER JOIN: ${handlerThread.state}")

        } catch (e: InterruptedException) {
            Log.e(TAG, "THREAD JOIN FAILED", e)
            Thread.currentThread().interrupt()
        } finally {
            Log.i(TAG, "CLEANING UP DONE!")
            Log.d(TAG, "AFTER CLEAN UP STATE: ${_handlerThread?.state}")
            _handlerThread?.uncaughtExceptionHandler = null
            _handlerThread = null
            _handler = null
        }
    }

    private fun detachOwner(owner: LifecycleOwner?) {
        if (owner == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "REMOVING OBSERVER ON MAIN LOOPER")
            owner.lifecycle.removeObserver(this@ThreadLifecycleControllerImpl)
        } else {
            Handler(Looper.getMainLooper()).post {
                if (_lifecycleOwner !== owner) {
                    Log.w(TAG, "REMOVING OBSERVER VIA HANDLER")
                    owner.lifecycle.removeObserver(this@ThreadLifecycleControllerImpl)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private val Thread.thId: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) threadId() else id
}
