package com.eva.feature_player.views

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import com.eva.utils.RecorderConstants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlinx.datetime.LocalTime

private const val TAG = "PLAYER_AMPLITUDE_GRAPH_2"
internal class PlayerAmplitudeGraph2View(context: Context) : TextureView(context),
	TextureView.SurfaceTextureListener {

    private val stateLock = java.lang.Object()
    private var state = WaveformRenderState()
    private var revision = 0L
    private var session: RenderSession? = null

	// swipe to scroll
	var isSwipeToScrollEnabled = false
	private var _onPlayPosChangeViaScroll: ((Float) -> Unit)? = null
	private var _onPlayPosChangeViaScrollEnd: (() -> Unit)? = null

	private val _gestureDetectorListener = GraphScrollListener(
		flingEnabled = false,
        totalContentWidthProvider = {
            synchronized(stateLock) {
                val pointDuration = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
                val durationSamples =
                    state.durationMs / pointDuration + if (state.durationMs % pointDuration == 0L) 0 else 1
                val samples = maxOf(state.data.size.toLong(), durationSamples)
                (samples.toDouble() * width / pointDuration).toFloat()
            }
        },
		onScrollEnd = { _onPlayPosChangeViaScrollEnd?.invoke() },
		onScroll = { ratio -> _onPlayPosChangeViaScroll?.invoke(ratio) },
	)

	private val _gestureDetector by lazy { GestureDetector(context, _gestureDetectorListener) }

	init {
		surfaceTextureListener = this
		isOpaque = true
		isClickable = true
	}

	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        stopRenderer()
        val renderer = PlayerWaveformRenderer(context).apply { resize(width, height) }
        val newSession = RenderSession(surface, Surface(surface), renderer)
        newSession.thread = Thread({ renderLoop(newSession) }, "player-waveform-renderer")
        synchronized(stateLock) {
            session = newSession
            markDirtyLocked()
            newSession.thread.start()
		}
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderer(surface)
		return true
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        synchronized(stateLock) { if (session?.texture === surface) markDirtyLocked() }
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onDetachedFromWindow() {
        stopRenderer()
        super.onDetachedFromWindow()
    }

	override fun performClick(): Boolean {
		super.performClick()
		return true
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (!isSwipeToScrollEnabled) return super.onTouchEvent(event)
		// if swipe to scroll enabled then only allow the gesture detection
		parent?.requestDisallowInterceptTouchEvent(true)
		val handleEvents = _gestureDetector.onTouchEvent(event)
		if (event.action == MotionEvent.ACTION_UP) performClick()
		when (event.actionMasked) {
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> _gestureDetectorListener.markScrollEnd()
			MotionEvent.ACTION_DOWN -> _gestureDetectorListener.markScrollStarted(
                event.x,
                synchronized(stateLock) { state.playRatio },
            )
		}
		// cancel touch events for the parent
		return handleEvents || super.onTouchEvent(event)
	}

    private fun renderLoop(active: RenderSession) {
        var renderedRevision = -1L
        try {
            while (active.running.get()) {
                val frame = synchronized(stateLock) {
                    while (active.running.get() && session === active && revision == renderedRevision) stateLock.wait()
                    if (!active.running.get() || session !== active) return
                    revision to state
                }
                val canvas = active.surface.lockCanvas(null)
				try {
                    if (active.running.get()) active.renderer.draw(canvas, frame.second)
                } finally {
                    active.surface.unlockCanvasAndPost(canvas)
                }
                renderedRevision = frame.first
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: RuntimeException) {
            if (active.running.get()) Log.e(TAG, "Unable to render waveform", e)
        } finally {
            active.renderer.release()
            active.surface.release()
        }
    }

    private fun markDirtyLocked() {
        revision++
        stateLock.notifyAll()
    }

    private fun stopRenderer(expectedTexture: SurfaceTexture? = null) {
        val old = synchronized(stateLock) {
            session?.takeIf { expectedTexture == null || it.texture === expectedTexture }?.also {
                it.running.set(false)
                session = null
                stateLock.notifyAll()
            }
        } ?: return
        old.thread.interrupt()
        if (old.thread !== Thread.currentThread()) {
			try {
                old.thread.join(250)
			} catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
	}

    private class RenderSession(
        val texture: SurfaceTexture,
        val surface: Surface,
        val renderer: PlayerWaveformRenderer,
    ) {
        val running = AtomicBoolean(true)
        lateinit var thread: Thread
	}

	fun onUpdateTrackDuration(duration: Duration) {
        val millis = duration.inWholeMilliseconds.coerceAtLeast(0L)
        synchronized(stateLock) {
            if (state.durationMs == millis) return
            state = state.copy(durationMs = millis)
            markDirtyLocked()
        }
	}

	fun onUpdateBookMarks(bookMarks: List<LocalTime>) {
        val times = bookMarks.map { it.toMillisecondOfDay() }.distinct().sorted()
        synchronized(stateLock) {
            if (state.bookmarks == times) return
            state = state.copy(bookmarks = times)
            markDirtyLocked()
		}
	}

	fun onUpdateGraphData(array: () -> FloatArray) {
        val values = array()
        synchronized(stateLock) {
            if (state.data.contentEquals(values)) return
            state = state.copy(data = values.copyOf())
            markDirtyLocked()
        }
	}

    fun onUpdateStyle(style: WaveformStyle) {
        synchronized(stateLock) {
            if (state.style == style) return
            state = state.copy(style = style)
            markDirtyLocked()
        }
    }

    fun onUpdatePadding(left: Int, top: Int, right: Int, bottom: Int) {
        val padding = WaveformPadding(left, top, right, bottom)
        synchronized(stateLock) {
            if (state.padding == padding) return
            state = state.copy(padding = padding)
            markDirtyLocked()
        }
        setPadding(left, top, right, bottom)
	}

	fun onUpdatePlayRatio(ratio: () -> Float) {
        val value = ratio().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
        synchronized(stateLock) {
            if (state.playRatio == value) return
            state = state.copy(playRatio = value)
            markDirtyLocked()
        }
	}

	fun onSwipeToChangeEnd(onScrollEndListener: () -> Unit) {
		_onPlayPosChangeViaScrollEnd = onScrollEndListener
	}

	fun onSwipeToChangePlayPosition(onScrollListener: (Float) -> Unit) {
		_onPlayPosChangeViaScroll = onScrollListener
	}

	fun cleanUp() {
        stopRenderer()
		_onPlayPosChangeViaScrollEnd = null
		_onPlayPosChangeViaScroll = null
	}
}
