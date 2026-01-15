package com.eva.feature_player.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.TextureView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.eva.ui.R
import com.eva.utils.RecorderConstants
import kotlinx.datetime.LocalTime
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.roundToInt
import kotlin.time.Duration

private const val TAG = "PLAYER_AMPLITUDE_GRAPH_2"
private const val TEXTURE_VIEW_TAG = "PLAYER_TEXTURE_LISTENER"

@OptIn(ExperimentalAtomicApi::class)
internal class PlayerAmplitudeGraph2View(context: Context) : TextureView(context),
	TextureView.SurfaceTextureListener {

	@Volatile
	private var _renderThread: Thread? = null

	@Volatile
	private var _isThRunning = false

	@Volatile
	private var _isDataAvailable = false

	// Cache for timeline
	private var _timelineCacheBitmap: Bitmap? = null
	private var _timelineCacheCanvas: Canvas? = null
	private val _timelineCached = AtomicBoolean(false)

	// Cache for graph data
	private var _graphCacheBitmap: Bitmap? = null
	private var _graphCacheCanvas: Canvas? = null

	@Volatile
	private var _cachedGraphDataSize = 0

	// core draw components
	@Volatile
	private var _graphData: FloatArray = floatArrayOf()
	private var _totalTrackDurationMillis: Long = 0L
	private var _playRatio: Float = 0f
	private val _bookMarkTimeStamps = CopyOnWriteArraySet<Int>()

	// colors and font
	var plotColor: Int = Color.WHITE
	var bookMarkColor: Int = Color.WHITE
	var timelineColor: Int = Color.WHITE
	var timelineColorVariant: Int = Color.WHITE
	var timelineTextColor: Int = Color.WHITE
	var trackPointerColor: Int = Color.WHITE
	var canvasBackground: Int = Color.BLACK
	var textTypeface: Typeface = Typeface.MONOSPACE
	var textFontSizeAsPx: Float = 12f

	// resources
	private val _bookMarkDrawable by lazy {
		ResourcesCompat.getDrawable(resources, R.drawable.ic_bookmark, null)
	}

	// swipe to scroll
	var isSwipeToScrollEnabled = false
	private var _onPlayPosChangeViaScroll: ((Float) -> Unit)? = null
	private var _onPlayPosChangeViaScrollEnd: (() -> Unit)? = null

	private val _gestureDetectorListener = GraphScrollListener(
		flingEnabled = false,
		totalContentWidthProvider = { _timelineCacheBitmap?.width?.toFloat() ?: 0f },
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
		_renderThread = Thread(renderLoop(), "thGraphRenderer_")
		_renderThread?.start()
		Log.d(TEXTURE_VIEW_TAG, "RENDERER THREAD IS ACTIVE")
		_isThRunning = true

		// init bitmap cache from the given width and height
		if (_graphCacheBitmap == null || _timelineCacheBitmap == null) {
			Log.d(TEXTURE_VIEW_TAG, "RE-INITIATE CACHED BITMAPS")
			initiateCacheBitmaps(width, height)
		}
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		try {
			_isThRunning = false
			_renderThread?.join(1000)
		} catch (e: Exception) {
			Log.e(TEXTURE_VIEW_TAG, "THREAD CLEANUP", e)
		}
		Log.d(TEXTURE_VIEW_TAG, "RENDERER THREAD IS CLEANED! ${_renderThread?.state}")
		// surface view is released
		return true
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		Log.d(TEXTURE_VIEW_TAG, "SURFACE TEXTURE SIZE CHANGED")
		// reinitiate bitmap cache as texture size changed
		initiateCacheBitmaps(width, height)
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

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
				_playRatio
			)
		}
		// cancel touch events for the parent
		return handleEvents || super.onTouchEvent(event)
	}

	private fun initiateCacheBitmaps(
		width: Int,
		height: Int,
		resetTimelineCache: Boolean = true,
		resetGraphCache: Boolean = true
	) {
		if (width <= 0 || height <= 0) {
			Log.w(TAG, "INVALID AREA | SIPPING UPDATE")
			return
		}

		// evaluate the sizes
		val maxSamples = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		val spikesWidth = width.toFloat() / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		val spikeSpace = (spikesWidth - dpToPx(1.5f)).let { amt ->
			if (amt > 0f) amt else dpToPx(2.0f)
		}
		val blockWidth = spikesWidth + spikeSpace

		val probableSampleSize = maxOf(_totalTrackDurationMillis / maxSamples, 10L)
		val maxWidth = (probableSampleSize * blockWidth + paddingLeft).roundToInt()
			.coerceAtLeast(width * 2)

		if (resetTimelineCache) {
			// recycle and clear the old bitmap
			_timelineCacheBitmap?.recycle()
			_timelineCacheBitmap = null
			// build a timeline based on the probable size
			_timelineCacheBitmap = createBitmap(maxWidth, height)
			_timelineCacheCanvas = Canvas(_timelineCacheBitmap!!)
			_timelineCached.store(false)
			_timelineCacheBitmap?.apply {
				Log.d(TAG, "TIMELINE CONTENT INVALIDATED NEW SIZE :${width} $height")
			}
		}

		if (resetGraphCache) {
			// clear the graph if any
			_graphCacheBitmap?.recycle()
			_graphCacheBitmap = null
			// build an arbitrary graph cache based on the probable size
			_graphCacheBitmap = createBitmap(maxWidth, height)
			_graphCacheCanvas = Canvas(_graphCacheBitmap!!)
			_cachedGraphDataSize = 0
			_graphCacheBitmap?.apply {
				Log.d(TAG, "GRAPH CONTENT INVALIDATED NEW SIZE :${width} $height")
			}
		}
	}

	private fun renderLoop(frameTimeMs: Long = 33L) = Runnable {
		while (_isThRunning) {
			val start = SystemClock.uptimeMillis()

			if (surfaceTexture?.isReleased == true) break
			// loop over the given canvas
			if (_isDataAvailable) {
				val canvas = lockCanvas() ?: continue
				try {
					canvas.drawFrame()
				} finally {
					_isDataAvailable = false
					unlockCanvasAndPost(canvas)
				}
			}

			val elapsed = SystemClock.uptimeMillis() - start
			val sleep = frameTimeMs - elapsed
			if (sleep <= 0) continue
			try {
				Thread.sleep(sleep)
			} catch (_: InterruptedException) {
				break
			}
		}
	}

	private fun Canvas.drawFrame() {
		if (width <= 0 || height <= 0) return

		drawColor(canvasBackground)

		val spikesWidth = width.toFloat() / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		val spikeGap = (spikesWidth - dpToPx(1.5f)).let { amt ->
			if (amt > 0f) amt else dpToPx(2.0f)
		}

		val probableSampleSize =
			_totalTrackDurationMillis / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		val sampleSize = maxOf(_graphData.size.toLong(), probableSampleSize)
		val totalSize = sampleSize * spikesWidth
		val translate = (width * 0.5f - paddingLeft) - (totalSize * _playRatio)

		// one time operation
		if (!_timelineCached.load() && _timelineCacheCanvas != null) {
			Log.d(TAG, "PREPARING TIME LINE")
			_timelineCacheCanvas?.drawTimeLineWithBookMarks(
				totalDurationInMillis = _totalTrackDurationMillis,
				bookMarks = _bookMarkTimeStamps,
				bookMarkDrawable = _bookMarkDrawable,
				outlineColor = timelineColor,
				outlineVariant = timelineColorVariant,
				textColor = timelineTextColor,
				bookMarkColor = bookMarkColor,
				typeface = textTypeface,
				spikesWidth = spikesWidth,
				imageSize = dpToPx(14f),
				textSizeInSp = textFontSizeAsPx,
				dpToPx = ::dpToPx,
				topPadding = paddingTop,
				bottomPadding = paddingBottom,
				leftPadding = paddingLeft
			)
			Log.d(TAG, "TIME TIMELINE DRAWING DONE!!")
			_timelineCached.store(true)
		}

		// Draw cached timeline
		withTranslation(x = translate) {
			_timelineCacheBitmap?.let { bitmap ->
				if (bitmap.isRecycled) return@withTranslation
				drawBitmap(bitmap, 0f, 0f, null)
			}
		}

		val currentDataSize = _graphData.size
		val cachedSize = _cachedGraphDataSize
		if (currentDataSize > cachedSize) {
			// Extract only the new data
			val newData = _graphData.sliceArray(cachedSize..<currentDataSize)

			// Draw only the new data to cache
			_graphCacheCanvas?.drawGraph(
				waves = newData,
				startIdx = cachedSize,
				spikesGap = spikeGap,
				spikesWidth = spikesWidth,
				color = plotColor,
				topPadding = paddingTop,
				bottomPadding = paddingBottom,
				leftPadding = paddingLeft
			)
			_cachedGraphDataSize = currentDataSize
		}

		// Draw cached graph
		withClip(
			left = paddingLeft - dpToPx(10f),
			right = width - paddingRight + dpToPx(10f),
			top = 0f,
			bottom = height.toFloat()
		) {
			withTranslation(translate) {
				_graphCacheBitmap?.let { bitmap ->
					if (bitmap.isRecycled) return@withTranslation
					drawBitmap(bitmap, 0f, 0f, null)
				}
			}
		}

		// Draw track pointer
		drawTrackPointer(
			xAxis = width * .5f,
			color = trackPointerColor,
			radius = spikesWidth,
			strokeWidth = spikesWidth,
			topPadding = paddingTop,
			bottomPadding = paddingBottom
		)
	}

	private fun dpToPx(dp: Float): Float =
		TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

	/**
	 *  Invalidate the timeline and graph.
	 *  Graph also need to be invalidated as we modify the graph bitmap size too
	 *  Now in special cases like when only the bookmarks are updated this is not need
	 */
	private fun invalidateTimeline(redrawGraph: Boolean = true) {
		_timelineCached.store(false)
		_isDataAvailable = true

		initiateCacheBitmaps(width, height, resetGraphCache = redrawGraph)
		Log.i(TAG, "REDRAWING TIMELINE")
	}

	/**
	 *  Invalidate only the graph.
	 *  Graph is only invalidated no timeline changes
	 */
	private fun resetGraphCache() {
		_graphCacheCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
		_cachedGraphDataSize = 0
		_isDataAvailable = true

		// invalidate the graph but keep the timeline
		initiateCacheBitmaps(width, height, resetTimelineCache = false)
		Log.i(TAG, "REDRAWING GRAPH")
	}

	fun onUpdateTrackDuration(duration: Duration) {
		val millis = duration.inWholeMilliseconds
		if (millis == _totalTrackDurationMillis) return
		_timelineCached.store(false)
		_totalTrackDurationMillis = millis

		// invalidate the timeline cache
		invalidateTimeline()
		Log.d(TAG, "TRACK DURATION UPDATED")
	}

	fun onUpdateBookMarks(bookMarks: List<LocalTime>) {
		val oldSize = _bookMarkTimeStamps.size
		// reset the bookmarks
		_bookMarkTimeStamps.clear()
		val sortedList = bookMarks.map { it.toMillisecondOfDay() }.sorted()
		_bookMarkTimeStamps.addAll(sortedList)

		// Only invalidate if bookmarks actually changed redraw the timeline
		if (_bookMarkTimeStamps.size != oldSize) {
			invalidateTimeline(redrawGraph = false)
			_isDataAvailable = true
			Log.d(TAG, "BOOKMARKS SIZE CHANGED")
		}
	}

	fun onUpdateGraphData(array: () -> FloatArray) {
		_isDataAvailable = true
		_graphData = array()
	}

	fun onUpdatePlotColor(color: Int) {
		if (plotColor == color) return
		plotColor = color
		resetGraphCache()
		Log.d(TAG, "PLOT COLOR UPDATED")
	}

	fun onUpdatePlayRatio(ratio: () -> Float) {
		_playRatio = ratio()
	}

	fun onSwipeToChangeEnd(onScrollEndListener: () -> Unit) {
		_onPlayPosChangeViaScrollEnd = onScrollEndListener
	}

	fun onSwipeToChangePlayPosition(onScrollListener: (Float) -> Unit) {
		_onPlayPosChangeViaScroll = onScrollListener
	}

	fun cleanUp() {
		if (_renderThread?.state != Thread.State.TERMINATED) {
			_renderThread?.join()
		}
		_renderThread = null
		// clean the bitmaps
		_timelineCacheBitmap?.recycle()
		_timelineCacheBitmap = null
		_graphCacheBitmap?.recycle()
		_graphCacheBitmap = null
		Log.d(TEXTURE_VIEW_TAG, "CLEANUP CODE CALLED")

		//clear callbacks
		_onPlayPosChangeViaScrollEnd = null
		_onPlayPosChangeViaScroll = null
		Log.d(TAG, "CALLBACKS REMOVED")
	}
}