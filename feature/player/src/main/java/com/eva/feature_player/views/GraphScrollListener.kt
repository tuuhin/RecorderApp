package com.eva.feature_player.views

import android.util.Log
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

private const val TAG = "GraphScrollListener"

internal class GraphScrollListener(
	private val totalContentWidthProvider: () -> Float,
	private val onScrollStart: (Float) -> Unit = {},
	private val onScrollEnd: () -> Unit = {},
	private val onScroll: (Float) -> Unit,
	private val flingEnabled: Boolean = true,
) : GestureDetector.SimpleOnGestureListener(), Choreographer.FrameCallback {

	private var _isScrolling = false
	private var _isFlinging = false
	private var _lastFrameTimeNanos = 0L

	private var _scrollRatioInternal = 0.0f
	private var _flingVelocity = 0f

	private val hitBoundary: Boolean
		get() = _scrollRatioInternal == 0f || _scrollRatioInternal == 1f

	override fun onDown(e: MotionEvent): Boolean {
		if (flingEnabled) stopFling()
		return true
	}

	override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float)
			: Boolean {
		// Convert pixel distance to ratio change
		val totalContentWidth = totalContentWidthProvider()
		if (totalContentWidth <= 0f) return false
		val deltaRatio = distanceX / totalContentWidth
		_scrollRatioInternal = (_scrollRatioInternal + deltaRatio).coerceIn(0f, 1f)
		onScroll(_scrollRatioInternal)
		return true
	}

	override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float)
			: Boolean {
		if (flingEnabled) startFling(velocityX)
		return true
	}

	override fun doFrame(frameTimeNanos: Long) {
		if (flingEnabled && !_isFlinging) return

		if (_lastFrameTimeNanos == 0L) {
			_lastFrameTimeNanos = frameTimeNanos
			// add the callback for the next frame
			Choreographer.getInstance().postFrameCallback(this)
			return
		}

		// Calculate delta time in seconds
		val dTInSec = (frameTimeNanos - _lastFrameTimeNanos) / 1_000_000_000f
		_lastFrameTimeNanos = frameTimeNanos

		// Update scroll position
		val previousRatio = _scrollRatioInternal
		_scrollRatioInternal = (previousRatio + _flingVelocity * dTInSec).coerceIn(0f, 1f)

		onScroll(_scrollRatioInternal)
		_flingVelocity *= 0.98f

		// slow down the fling velocity until it's too slow
		if (abs(_flingVelocity) < 0.01f || hitBoundary) {
			stopFling()
			return
		}
		// On each frame we include the callback so convert the fling velocity to cancellations
		Choreographer.getInstance().postFrameCallback(this)
	}

	private fun pxToRatioVelocity(velocityX: Float): Float {
		val totalContentWidth = totalContentWidthProvider()
		if (totalContentWidth <= 0f) return 0f
		return -(velocityX / totalContentWidth) * 1.5f
	}

	private fun startFling(velocityX: Float) {
		stopFling()

		_flingVelocity = pxToRatioVelocity(velocityX)
		_isFlinging = true
		_lastFrameTimeNanos = 0L
		Log.d(TAG, "FLING STARTED: VELOCITY :$velocityX")
		Choreographer.getInstance().postFrameCallback(this)
	}

	private fun stopFling() {
		if (!_isFlinging) return

		_isFlinging = false
		_flingVelocity = 0f
		_lastFrameTimeNanos = 0L

		Log.d(TAG, "FLING STOPPED")
		// stop the frame callback
		Choreographer.getInstance().removeFrameCallback(this)
		// now call scroll end
		onScrollEnd()
	}

	fun markScrollEnd() {
		_isScrolling = false
		Log.d(TAG, "SCROLL ENDED")
		// don't call scroll end if flinging is on
		if (_isFlinging) return
		onScrollEnd()
	}

	fun markScrollStarted(positionX: Float, scrollRatio: Float) {
		_isScrolling = true
		stopFling()
		Log.d(TAG, "SCROLL STARTED =$positionX")
		onScrollStart(positionX)
		_scrollRatioInternal = scrollRatio
	}
}