package com.eva.recorder.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.cash.turbine.test
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecorderServiceBinderImplTest {

    private lateinit var context: Context
    private lateinit var binderImpl: RecorderServiceBinderImpl
    private val connectionSlot = slot<ServiceConnection>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every {
            context.bindService(any<Intent>(), capture(connectionSlot), any<Int>())
        } returns true

        binderImpl = RecorderServiceBinderImpl(context)
    }

    @Test
    fun `given uninitialized binder when bindToService then bindService is invoked on context`() {
        binderImpl.bindToService()

        verify(exactly = 1) {
            context.bindService(any<Intent>(), connectionSlot.captured, Context.BIND_AUTO_CREATE)
        }
    }

    @Test
    fun `given already bound when bindToService again then duplicate bind is ignored`() {
        binderImpl.bindToService()
        binderImpl.bindToService()

        verify(exactly = 1) {
            context.bindService(any<Intent>(), any(), Context.BIND_AUTO_CREATE)
        }
    }

    private interface TestBinderData : IBinder, RecorderDataInterface

    @Test
    fun `given service bound when onServiceConnected then connection is ready and service flows are emitted`() =
        runTest {
            binderImpl.bindToService()

            val mockDataInterface = mockk<TestBinderData>()
            val testTime = LocalTime(1, 23)
            val testState = RecorderState.RECORDING
            val testBookmarks = setOf(5.seconds)
            val testAmplitudes = listOf(RecordedPoint(timeInMillis = 100L, rmsValue = 20f))
            val testTranscript = "Hello World"

            every { mockDataInterface.recorderTime } returns flowOf(testTime)
            every { mockDataInterface.recorderState } returns MutableStateFlow(testState)
            every { mockDataInterface.bookMarks } returns flowOf(testBookmarks)
            every { mockDataInterface.amplitudes } returns flowOf(testAmplitudes)
            every { mockDataInterface.transcript } returns flowOf(testTranscript)

            binderImpl.recorderTimer.test {
                // Initially fallback
                assertEquals(LocalTime(0, 0), awaitItem())

                connectionSlot.captured.onServiceConnected(mockk(), mockDataInterface)

                assertEquals(testTime, awaitItem())
                assertTrue(binderImpl.isConnectionReady.value)
            }
        }

    @Test
    fun `given connected service when unBindService then unbinds and flows revert to defaults`() = runTest {
        binderImpl.bindToService()

        val mockDataInterface = mockk<TestBinderData>()
        val stateFlow = MutableStateFlow(RecorderState.RECORDING)
        every { mockDataInterface.recorderTime } returns flowOf(LocalTime(1, 23))
        every { mockDataInterface.recorderState } returns stateFlow
        every { mockDataInterface.bookMarks } returns flowOf(emptySet())
        every { mockDataInterface.amplitudes } returns flowOf(emptyList())
        every { mockDataInterface.transcript } returns flowOf("")

        binderImpl.recorderState.test {
            assertEquals(RecorderState.IDLE, awaitItem())

            connectionSlot.captured.onServiceConnected(mockk(), mockDataInterface)
            assertEquals(RecorderState.RECORDING, awaitItem())
            assertTrue(binderImpl.isConnectionReady.value)

            binderImpl.unBindService()

            assertEquals(RecorderState.IDLE, awaitItem())
            assertFalse(binderImpl.isConnectionReady.value)

            verify(exactly = 1) { context.unbindService(connectionSlot.captured) }
        }
    }

    @Test
    fun `given bind in flight when unBindService called before connected then connection is unbound immediately on delivery`() {
        binderImpl.bindToService()

        // Unbind while in flight
        binderImpl.unBindService()
        verify(exactly = 1) { context.unbindService(connectionSlot.captured) }

        // Now onServiceConnected fires asynchronously
        val mockDataInterface = mockk<TestBinderData>()
        connectionSlot.captured.onServiceConnected(mockk(), mockDataInterface)

        // Context unbind should have been called again for the orphaned connection
        verify(exactly = 2) { context.unbindService(connectionSlot.captured) }
        assertFalse(binderImpl.isConnectionReady.value)
    }

    @Test
    fun `given service dies via onServiceDisconnected then connection ready resets to false and flows revert`() =
        runTest {
            binderImpl.bindToService()

            val mockDataInterface = mockk<TestBinderData>()
            val testTimeFlow = MutableSharedFlow<LocalTime>()
            every { mockDataInterface.recorderTime } returns testTimeFlow
            every { mockDataInterface.recorderState } returns MutableStateFlow(RecorderState.RECORDING)
            every { mockDataInterface.bookMarks } returns flowOf(emptySet())
            every { mockDataInterface.amplitudes } returns flowOf(emptyList())
            every { mockDataInterface.transcript } returns flowOf("")

            binderImpl.recorderTimer.test {
                assertEquals(LocalTime(0, 0), awaitItem())

                connectionSlot.captured.onServiceConnected(mockk(), mockDataInterface)

                testTimeFlow.emit(LocalTime(0, 5))
                assertEquals(LocalTime(0, 5), awaitItem())

                connectionSlot.captured.onServiceDisconnected(ComponentName("pkg", "cls"))

                assertEquals(LocalTime(0, 0), awaitItem())
                assertFalse(binderImpl.isConnectionReady.value)
            }
        }

    @Test
    fun `given service binding dies via onBindingDied then state resets`() {
        binderImpl.bindToService()

        val mockDataInterface = mockk<TestBinderData>()
        connectionSlot.captured.onServiceConnected(mockk(), mockDataInterface)
        assertTrue(binderImpl.isConnectionReady.value)

        connectionSlot.captured.onBindingDied(ComponentName("pkg", "cls"))
        assertFalse(binderImpl.isConnectionReady.value)
    }

    @Test
    fun `given bindService throws SecurityException then unbind is not invoked`() {
        every {
            context.bindService(any<Intent>(), any(), any<Int>())
        } throws SecurityException("Permission denied")

        binderImpl.bindToService()

        assertFalse(binderImpl.isConnectionReady.value)
        verify(exactly = 0) { context.unbindService(any()) }
    }

    @Test
    fun `given not bound when unBindService called then no unbind call made`() {
        binderImpl.unBindService()

        verify(exactly = 0) { context.unbindService(any()) }
    }

    @Test
    fun `given cleanUp called then unbinds and clears state`() {
        binderImpl.bindToService()
        binderImpl.cleanUp()

        verify(exactly = 1) { context.unbindService(connectionSlot.captured) }
        assertFalse(binderImpl.isConnectionReady.value)
    }
}
