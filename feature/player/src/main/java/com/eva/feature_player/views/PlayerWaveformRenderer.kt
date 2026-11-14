package com.eva.feature_player.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.eva.ui.R
import com.eva.utils.RecorderConstants
import kotlin.math.ceil
import kotlin.math.floor

internal data class WaveformStyle(
    val plotColor: Int = Color.WHITE,
    val bookmarkColor: Int = Color.WHITE,
    val timelineColor: Int = Color.WHITE,
    val timelineVariant: Int = Color.WHITE,
    val timelineTextColor: Int = Color.WHITE,
    val pointerColor: Int = Color.WHITE,
    val background: Int = Color.BLACK,
    val typeface: Typeface = Typeface.MONOSPACE,
    val textSizePx: Float = 12f,
)

internal data class WaveformPadding(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

internal data class WaveformRenderState(
    val data: FloatArray = floatArrayOf(),
    val durationMs: Long = 0,
    val playRatio: Float = 0f,
    val bookmarks: List<Int> = emptyList(),
    val style: WaveformStyle = WaveformStyle(),
    val padding: WaveformPadding = WaveformPadding(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaveformRenderState

        if (durationMs != other.durationMs) return false
        if (playRatio != other.playRatio) return false
        if (!data.contentEquals(other.data)) return false
        if (bookmarks != other.bookmarks) return false
        if (style != other.style) return false
        if (padding != other.padding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationMs.hashCode()
        result = 31 * result + playRatio.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + bookmarks.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + padding.hashCode()
        return result
    }
}

internal class PlayerWaveformRenderer(private val context: Context) {
    private var timelineBitmap: Bitmap? = null
    private var graphBitmap: Bitmap? = null
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var timelineKey: TimelineKey? = null
    private var graphKey: GraphKey? = null
    private val bookmarkDrawable by lazy {
        ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ic_bookmark,
            null,
        )
    }

    fun resize(width: Int, height: Int) {
        if (bitmapWidth == width && bitmapHeight == height) return
        releaseBitmaps()
        bitmapWidth = width
        bitmapHeight = height
        if (width > 0 && height > 0 && width.toLong() * height <= MAX_BITMAP_PIXELS) {
            try {
                timelineBitmap = createBitmap(width, height)
                graphBitmap = createBitmap(width, height)
            } catch (e: OutOfMemoryError) {
                releaseBitmaps()
                Log.w("PLAYER_WAVEFORM", "Drawing directly without bitmap caches", e)
            }
        }
    }

    fun draw(canvas: Canvas, state: WaveformRenderState) {
        if (canvas.width <= 0 || canvas.height <= 0) return
        resize(canvas.width, canvas.height)
        val width = canvas.width
        val spikesWidth = width.toFloat() / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
        val sampleDuration = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE.toLong()
        val durationSamples = state.durationMs / sampleDuration + if (state.durationMs % sampleDuration == 0L) 0 else 1
        val sampleCount = maxOf(state.data.size.toLong(), durationSamples)
        val contentWidth = sampleCount.toDouble() * spikesWidth
        val translation = (width * 0.5 - state.padding.left - contentWidth * state.playRatio).toFloat()
        val spacing = spikesWidth / sampleDuration
        val margin = dpToPx(64f)
        val startMs = floor((-translation - state.padding.left - margin) / spacing).toLong().coerceAtLeast(0)
        val endMs = ceil((width - translation - state.padding.left + margin) / spacing).toLong().coerceAtLeast(0)
        val startIndex =
            floor((-translation - state.padding.left - margin) / spikesWidth).toInt().coerceIn(0, state.data.size)
        val endIndex =
            ceil((width - translation - state.padding.left + margin) / spikesWidth).toInt().coerceIn(0, state.data.size)
        val gap = (spikesWidth - dpToPx(1.5f)).let { if (it > 0f) it else dpToPx(2f) }

        canvas.drawColor(state.style.background)
        val newTimelineKey = TimelineKey(
            width,
            canvas.height,
            state.durationMs,
            state.playRatio,
            sampleCount,
            state.bookmarks,
            state.style,
            state.padding,
        )
        val timeline = timelineBitmap
        if (timeline == null) {
            drawTimeline(canvas, state, translation, spikesWidth, startMs, endMs)
        } else {
            if (timelineKey != newTimelineKey) {
                timeline.eraseColor(Color.TRANSPARENT)
                drawTimeline(Canvas(timeline), state, translation, spikesWidth, startMs, endMs)
                timelineKey = newTimelineKey
            }
            canvas.drawBitmap(timeline, 0f, 0f, null)
        }

        val newGraphKey = GraphKey(
            width = width,
            height = canvas.height,
            data = state.data,
            duration = state.durationMs,
            ratio = state.playRatio,
            style = state.style,
            padding = state.padding,
        )
        val graph = graphBitmap
        if (graph == null) {
            drawWaveform(canvas, state, translation, spikesWidth, gap, startIndex, endIndex)
        } else {
            if (graphKey != newGraphKey) {
                graph.eraseColor(Color.TRANSPARENT)
                drawWaveform(Canvas(graph), state, translation, spikesWidth, gap, startIndex, endIndex)
                graphKey = newGraphKey
            }
            canvas.drawBitmap(graph, 0f, 0f, null)
        }
        canvas.drawTrackPointer(
            width * .5f,
            state.style.pointerColor,
            spikesWidth,
            spikesWidth,
            state.padding.top,
            state.padding.bottom,
        )
    }

    private fun drawTimeline(
        canvas: Canvas,
        state: WaveformRenderState,
        translation: Float,
        spikesWidth: Float,
        startMs: Long,
        endMs: Long,
    ) {
        canvas.withTranslation(translation) {
            drawTimeLineWithBookMarks(
                totalDurationInMillis = state.durationMs,
                bookMarks = state.bookmarks,
                bookMarkDrawable = bookmarkDrawable,
                outlineColor = state.style.timelineColor,
                outlineVariant = state.style.timelineVariant,
                textColor = state.style.timelineTextColor,
                bookMarkColor = state.style.bookmarkColor,
                typeface = state.style.typeface,
                spikesWidth = spikesWidth,
                imageSize = dpToPx(14f),
                textSizeInSp = state.style.textSizePx,
                dpToPx = ::dpToPx,
                topPadding = state.padding.top,
                bottomPadding = state.padding.bottom,
                leftPadding = state.padding.left,
                visibleStartMillis = startMs,
                visibleEndMillis = endMs,
            )
        }
    }

    private fun drawWaveform(
        canvas: Canvas,
        state: WaveformRenderState,
        translation: Float,
        spikesWidth: Float,
        gap: Float,
        start: Int,
        end: Int,
    ) {
        canvas.withClip(
            left = state.padding.left - dpToPx(10f), top = 0f,
            right = canvas.width - state.padding.right + dpToPx(10f), bottom = canvas.height.toFloat(),
        ) {
            withTranslation(translation) {
                drawGraph(
                    state.data,
                    gap,
                    spikesWidth,
                    state.style.plotColor,
                    sourceStartIdx = start,
                    sourceEndIdx = end,
                    topPadding = state.padding.top,
                    bottomPadding = state.padding.bottom,
                    leftPadding = state.padding.left,
                )
            }
        }
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)

    fun release() {
        releaseBitmaps()
        bitmapWidth = 0
        bitmapHeight = 0
    }

    private fun releaseBitmaps() {
        timelineBitmap?.recycle()
        graphBitmap?.recycle()
        timelineBitmap = null
        graphBitmap = null
        timelineKey = null
        graphKey = null
    }

    private data class TimelineKey(
        val width: Int, val height: Int, val duration: Long, val ratio: Float, val samples: Long,
        val bookmarks: List<Int>, val style: WaveformStyle, val padding: WaveformPadding,
    )

    private class GraphKey(
        val width: Int, val height: Int, val data: FloatArray, val duration: Long, val ratio: Float,
        val style: WaveformStyle, val padding: WaveformPadding,
    )

    private companion object {
        const val MAX_BITMAP_PIXELS = 4_000_000L
    }
}
