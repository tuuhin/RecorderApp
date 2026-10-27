package com.eva.recorder_native

import android.Manifest
import android.content.Context
import androidx.media3.common.C
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.test.utils.TestUtil
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class WavAudioRecorderTest {

	@get:Rule
	val rule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

	@get:Rule
	val tmpFolder = TemporaryFolder()

	private lateinit var recorder: WavAudioRecorder
	private lateinit var context: Context
	private lateinit var outputFolder: File

	@BeforeTest
	fun setup() {
		recorder = WavAudioRecorder()
		context = InstrumentationRegistry.getInstrumentation().targetContext
		outputFolder = tmpFolder.newFolder("recorder-native-tests")
	}

	@AfterTest
	fun tearDown() {
		recorder.release()
	}

	@Test
	fun createThenReleaseReportsNotRecordingAndReleaseIsIdempotent() {
		recorder.create()

		assertThat(recorder.isRecording).isFalse()

		recorder.release()
		recorder.release()
	}

	@Test
	fun createTwiceThrowsRecorderAlreadyInitiated() {
		recorder.create()

		assertFailsWith<RecorderAlreadyInitiatedException> { recorder.create() }
	}

	@Test
	fun everyMethodBeforeCreateThrowsRecorderHandleException() {
		val file = newOutputFile("before-create.wav")

		assertFailsWith<RecorderHandleException> { recorder.isRecording }
		assertFailsWith<RecorderHandleException> { recorder.setOutputFile(file) }
		assertFailsWith<RecorderHandleException> { recorder.setRecorderConfig(RecorderConfig()) }
		assertFailsWith<RecorderHandleException> { recorder.prepare() }
		assertFailsWith<RecorderHandleException> { recorder.start() }
		assertFailsWith<RecorderHandleException> { recorder.stop() }
		assertFailsWith<RecorderHandleException> { recorder.pause() }
		assertFailsWith<RecorderHandleException> { recorder.resume() }
		assertFailsWith<RecorderHandleException> { recorder.reset() }
	}

	@Test
	fun recordAndStopProducesReadableWavFile() = runBlocking {
		val file = newOutputFile("recorded.wav")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		delay(RECORD_DURATION_MS)

		assertTrue(recorder.isRecording)

		recorder.stop()

		assertEquals(false, recorder.isRecording)

		assertReadableWav(file, channelCount = 1)

		recorder.release()
	}

	@Test
	fun pauseAndResumeToggleRecordingState() = runBlocking {
		val file = newOutputFile("paused.wav")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		delay(RECORD_DURATION_MS)

		recorder.pause()

		assertEquals(false, recorder.isRecording)

		recorder.resume()

		assertTrue(recorder.isRecording)

		delay(RECORD_DURATION_MS)

		recorder.stop()

		assertEquals(false, recorder.isRecording)

		recorder.release()
	}

	@Test
	fun resetAllowsRecordingToANewFile() = runBlocking {
		val first = newOutputFile("reset-first.wav")
		val second = newOutputFile("reset-second.wav")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))

		recorder.setOutputFile(first)
		recorder.prepare()
		recorder.start()
		delay(RECORD_DURATION_MS)

		recorder.stop()

		recorder.reset()

		recorder.setOutputFile(second)
		recorder.prepare()
		recorder.start()
		delay(RECORD_DURATION_MS)

		recorder.stop()

		assertReadableWav(second, channelCount = 1)

		recorder.release()
	}

	private fun assertReadableWav(file: File, channelCount: Int) {
		val output = TestUtil.extractAllSamplesFromFilePath(WavExtractor(), file.absolutePath)

		assertThat(output.numberOfTracks).isEqualTo(1)

		val seekMap = output.seekMap
		assertThat(seekMap).isNotNull()
		assertThat(seekMap.durationUs).isGreaterThanOrEqualTo(0)

		val track = output.trackOutputs.valueAt(0)
		val format = track.lastFormat

		assertThat(format?.sampleRate).isNotNull().isGreaterThan(0)
		assertThat(channelCount).isEqualTo(format?.channelCount)
		assertThat(C.ENCODING_PCM_16BIT).isEqualTo(format?.pcmEncoding)
		assertThat(track.sampleCount).isGreaterThan(0)
	}

	private fun newOutputFile(name: String): File = File(outputFolder, name)

	companion object {

		private const val SAMPLE_RATE = 44_100
		private val RECORD_DURATION_MS = 1_500.milliseconds
	}
}
