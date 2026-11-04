package com.eva.feature_player.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerAmplitudeGraph2ViewTest {

    @Test
    fun givenSameLengthWaveformWhenValuesChangeThenGraphIsRedrawn() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = PlayerAmplitudeGraph2View(InstrumentationRegistry.getInstrumentation().targetContext)
            val renderer = PlayerWaveformRenderer(view.context)
            view.layout(0, 0, 400, 200)
            view.onUpdateStyle(WaveformStyle(plotColor = Color.RED))
            view.onUpdateTrackDuration(10.seconds)
            view.onUpdatePlayRatio { 0.5f }
            val state = view.javaClass.getDeclaredField("state").apply { isAccessible = true }
            val before = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
            val after = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)

            try {
                view.onUpdateGraphData { FloatArray(100) }
                renderer.draw(Canvas(before), state.get(view) as WaveformRenderState)
                view.onUpdateGraphData { FloatArray(100) { 1f } }
                renderer.draw(Canvas(after), state.get(view) as WaveformRenderState)
                assertFalse("Changed waveform values must redraw the graph", before.sameAs(after))
            } finally {
                view.cleanUp()
                renderer.release()
                before.recycle()
                after.recycle()
            }
        }
    }

    @Test
    fun givenMovedBookmarkWithSameCountWhenUpdatedThenTimelineChanges() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = PlayerAmplitudeGraph2View(InstrumentationRegistry.getInstrumentation().targetContext)
            val renderer = PlayerWaveformRenderer(view.context)
            view.layout(0, 0, 400, 200)
            view.onUpdateStyle(WaveformStyle(bookmarkColor = Color.GREEN))
            view.onUpdateTrackDuration(10.seconds)
            view.onUpdatePlayRatio { 0.5f }
            val state = view.javaClass.getDeclaredField("state").apply { isAccessible = true }
            val before = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
            val after = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
            try {
                view.onUpdateBookMarks(listOf(LocalTime(0, 0, 4)))
                renderer.draw(Canvas(before), state.get(view) as WaveformRenderState)
                view.onUpdateBookMarks(listOf(LocalTime(0, 0, 6)))
                renderer.draw(Canvas(after), state.get(view) as WaveformRenderState)
                assertFalse(before.sameAs(after))
            } finally {
                view.cleanUp()
                renderer.release()
                before.recycle()
                after.recycle()
            }
        }
    }

    @Test
    fun givenPlaybackAndStyleChangesWhenRenderedThenViewportUpdates() {
        val renderer = PlayerWaveformRenderer(InstrumentationRegistry.getInstrumentation().targetContext)
        val before = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val moved = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val recolored = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val data = FloatArray(100) { if (it < 50) 1f else 0f }
        val state = WaveformRenderState(
            data = data, durationMs = 10_000, playRatio = 0.2f,
            style = WaveformStyle(plotColor = Color.RED),
        )
        try {
            renderer.draw(Canvas(before), state)
            renderer.draw(Canvas(moved), state.copy(playRatio = 0.8f))
            renderer.draw(
                Canvas(recolored),
                state.copy(playRatio = 0.8f, style = state.style.copy(plotColor = Color.BLUE)),
            )
            assertFalse(before.sameAs(moved))
            assertFalse(moved.sameAs(recolored))
        } finally {
            renderer.release()
            before.recycle()
            moved.recycle()
            recolored.recycle()
        }
    }

    @Test
    fun givenRenormalizedPrefixWhenSamplesAreAppendedThenEarlierSamplesRedraw() {
        val renderer = PlayerWaveformRenderer(InstrumentationRegistry.getInstrumentation().targetContext)
        val before = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val after = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val state = WaveformRenderState(
            data = FloatArray(100) { 0.2f }, durationMs = 20_000, playRatio = 0.25f,
            style = WaveformStyle(plotColor = Color.RED),
        )
        try {
            renderer.draw(Canvas(before), state)
            renderer.draw(Canvas(after), state.copy(data = FloatArray(150) { 0.8f }))
            assertFalse(before.sameAs(after))
        } finally {
            renderer.release()
            before.recycle()
            after.recycle()
        }
    }

    @Test
    fun givenLongRecordingWhenResizedThenCachesStayViewportSizedAndAreRecreated() {
        val renderer = PlayerWaveformRenderer(InstrumentationRegistry.getInstrumentation().targetContext)
        val first = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val second = Bitmap.createBitmap(500, 250, Bitmap.Config.ARGB_8888)
        try {
            val state = WaveformRenderState(durationMs = 24L * 60 * 60 * 1_000, playRatio = 0.5f)
            renderer.draw(Canvas(first), state)
            val field = renderer.javaClass.getDeclaredField("timelineBitmap").apply { isAccessible = true }
            val oldCache = field.get(renderer) as Bitmap
            assertEquals(400, oldCache.width)
            assertEquals(200, oldCache.height)
            renderer.draw(Canvas(second), state)
            assertTrue(oldCache.isRecycled)
            val newCache = field.get(renderer) as Bitmap
            assertEquals(500, newCache.width)
            assertEquals(250, newCache.height)
        } finally {
            renderer.release()
            first.recycle()
            second.recycle()
        }
    }

    @Test
    fun givenSurfaceRecreationWhenDataArrivesBeforeVisibilityThenBothSessionsRenderAndStop() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var view: PlayerAmplitudeGraph2View
        instrumentation.runOnMainSync {
            view = PlayerAmplitudeGraph2View(instrumentation.targetContext)
            view.layout(0, 0, 400, 200)
            view.onUpdateTrackDuration(10.seconds)
            view.onUpdateGraphData { FloatArray(100) { 0.7f } }
        }

        repeat(2) {
            val frame = CountDownLatch(1)
            val texture = SurfaceTexture(false).apply {
                setDefaultBufferSize(400, 200)
                setOnFrameAvailableListener({ frame.countDown() }, Handler(Looper.getMainLooper()))
            }
            try {
                instrumentation.runOnMainSync { view.onSurfaceTextureAvailable(texture, 400, 200) }
                assertTrue("Surface did not receive its initial frame", frame.await(5, TimeUnit.SECONDS))
                val sessionField = view.javaClass.getDeclaredField("session").apply { isAccessible = true }
                val session = checkNotNull(sessionField.get(view))
                val threadField = session.javaClass.getDeclaredField("thread").apply { isAccessible = true }
                val thread = threadField.get(session) as Thread
                assertTrue(thread.isAlive)
                val start = SystemClock.uptimeMillis()
                instrumentation.runOnMainSync { view.onSurfaceTextureDestroyed(texture) }
                assertTrue(SystemClock.uptimeMillis() - start < 1_000)
                assertFalse(thread.isAlive)
            } finally {
                instrumentation.runOnMainSync { view.cleanUp() }
                texture.release()
            }
        }
    }

    @Test
    fun givenReplacementSurfaceWhenOldSurfaceIsDestroyedThenNewSessionStaysActive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var view: PlayerAmplitudeGraph2View
        val oldFrame = CountDownLatch(1)
        val newFrame = CountDownLatch(1)
        val oldTexture = SurfaceTexture(false).apply {
            setDefaultBufferSize(400, 200)
            setOnFrameAvailableListener({ oldFrame.countDown() }, Handler(Looper.getMainLooper()))
        }
        val newTexture = SurfaceTexture(false).apply {
            setDefaultBufferSize(400, 200)
            setOnFrameAvailableListener({ newFrame.countDown() }, Handler(Looper.getMainLooper()))
        }
        try {
            instrumentation.runOnMainSync {
                view = PlayerAmplitudeGraph2View(instrumentation.targetContext)
                view.layout(0, 0, 400, 200)
                view.onSurfaceTextureAvailable(oldTexture, 400, 200)
            }
            assertTrue(oldFrame.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { view.onSurfaceTextureAvailable(newTexture, 400, 200) }
            assertTrue(newFrame.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { view.onSurfaceTextureDestroyed(oldTexture) }
            val sessionField = view.javaClass.getDeclaredField("session").apply { isAccessible = true }
            val session = checkNotNull(sessionField.get(view))
            val threadField = session.javaClass.getDeclaredField("thread").apply { isAccessible = true }
            assertTrue((threadField.get(session) as Thread).isAlive)
        } finally {
            instrumentation.runOnMainSync { view.cleanUp() }
            oldTexture.release()
            newTexture.release()
        }
    }
}
