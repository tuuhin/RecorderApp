package com.eva.recorder_native

import android.Manifest
import android.content.Context
import androidx.media3.common.C
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.test.utils.TestUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FlacAudioRecorderTest {

	@get:Rule
	val rule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

	@get:Rule
	val tmpFolder = TemporaryFolder()

	private lateinit var recorder: FlacAudioRecorder
	private lateinit var context: Context
	private lateinit var outputFolder: File

	@BeforeTest
	fun setup() {
		recorder = FlacAudioRecorder()
		context = InstrumentationRegistry.getInstrumentation().targetContext
		outputFolder = tmpFolder.newFolder("recorder-native-tests")
	}

	@AfterTest
	fun tearDown() {
		recorder.release()
	}

	@Test
	fun create_then_release_reports_not_recording_and_release_is_idempotent() {
		recorder.create()

		assertThat(recorder.isRecording).isFalse()

		recorder.release()
		recorder.release()
	}

	@Test
	fun create_twice_throws_recorder_already_initiated() {
		recorder.create()

		assertFailsWith<RecorderAlreadyInitiatedException> { recorder.create() }
	}

	@Test
	fun every_method_before_create_throws_recorder_handle_exception() {
		val file = newOutputFile("before-create.flac")

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
	fun invalid_config_values_throw_illegal_argument() {
		recorder.create()

		assertFailsWith<IllegalArgumentException> {
			recorder.setRecorderConfig(RecorderConfig(sampleRate = 0, channel = 1))
		}
		assertFailsWith<IllegalArgumentException> {
			recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 0))
		}
		assertFailsWith<IllegalArgumentException> {
			recorder.setRecorderConfig(RecorderConfig(sampleRate = -1, channel = 1))
		}
	}

	@Test
	fun start_before_prepare_throws_illegal_state() {
		recorder.create()

		assertFailsWith<IllegalStateException> { recorder.start() }
	}

	@Test
	fun prepare_without_output_file_throws_illegal_state_and_is_retryable() {
		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))

		assertFailsWith<IllegalStateException> { recorder.prepare() }

		// The failed prepare must not leave the recorder unusable.
		recorder.setOutputFile(newOutputFile("prepare-retry.flac"))
		recorder.prepare()

		assertThat(recorder.isRecording).isFalse()
	}

	@Test
	fun prepare_locks_output_file_and_config() {
		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(newOutputFile("locked.flac"))
		recorder.prepare()

		assertFailsWith<IllegalStateException> { recorder.prepare() }
		assertFailsWith<IllegalStateException> { recorder.setOutputFile(newOutputFile("other.flac")) }
		assertFailsWith<IllegalStateException> { recorder.setRecorderConfig(RecorderConfig()) }
	}

	@Test
	fun stop_before_prepare_is_silent_no_op() {
		recorder.create()

		// Nothing has been opened yet; stop has nothing to clean up.
		recorder.stop()
	}

	@Test
	fun record_and_stop_produces_readable_flac_file() = runBlocking {
		val file = newOutputFile("recorded.flac")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		delay(RECORD_DURATION_MS)

		assertThat(recorder.isRecording).isTrue()

		recorder.stop()

		// Stopping an already stopped recorder must not throw.
		recorder.stop()

		assertThat(recorder.isRecording).isFalse()

		assertReadableFlac(file, channelCount = 1)

		recorder.release()
	}

	@Test
	fun pause_and_resume_toggle_recording_state() = runBlocking {
		val file = newOutputFile("paused.flac")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		delay(RECORD_DURATION_MS)

		recorder.pause()
		assertThat(recorder.isRecording).isFalse()

		recorder.resume()
		assertThat(recorder.isRecording).isTrue()

		delay(RECORD_DURATION_MS)

		recorder.stop()
		assertThat(recorder.isRecording).isFalse()

		assertReadableFlac(file)

		recorder.release()
	}

	@Test
	fun pause_without_recording_throws_illegal_state() {
		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(newOutputFile("not-recording.flac"))
		recorder.prepare()

		assertFailsWith<IllegalStateException> { recorder.pause() }
	}

	@Test
	fun resume_while_recording_throws_illegal_state() {
		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(newOutputFile("resume-conflict.flac"))
		recorder.prepare()
		recorder.start()

		assertThat(recorder.isRecording).isTrue()

		assertFailsWith<IllegalStateException> { recorder.resume() }

		recorder.stop()
	}

	@Test
	fun reset_allows_recording_to_a_new_file() = runBlocking {
		val first = newOutputFile("reset-first.flac")
		val second = newOutputFile("reset-second.flac")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))

		recorder.setOutputFile(first)
		recorder.prepare()
		recorder.start()
		delay(RECORD_DURATION_MS)

		recorder.stop()

		// The previous recording is finalized; starting again requires reset.
		assertFailsWith<IllegalStateException> { recorder.start() }

		recorder.reset()

		recorder.setOutputFile(second)
		recorder.prepare()
		recorder.start()
		delay(RECORD_DURATION_MS)

		recorder.stop()

		assertReadableFlac(first)
		assertReadableFlac(second)

		recorder.release()
	}

	@Test
	fun immediate_stop_still_produces_parseable_file() {
		val file = newOutputFile("immediate-stop.flac")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		// Stop before any meaningful amount of audio was captured.
		recorder.stop()

		assertThat(recorder.isRecording).isFalse()
		assertThat(file.exists()).isTrue()

		// The stop races the first audio callback, so the file may legitimately
		// hold zero frames. media3's FlacExtractor cannot parse a zero-frame
		// FLAC (EOFException in FlacMetadataReader), but the container itself
		// must still be finalized and structurally valid.
		assertStructurallyValidFlac(file, sampleRate = SAMPLE_RATE, channelCount = 1)
	}

	@Test
	fun create_after_release_records_again() = runBlocking {
		recorder.create()
		recorder.release()

		// After release the recorder behaves like a fresh instance
		// that has not been created yet.
		assertFailsWith<RecorderHandleException> { recorder.isRecording }
		assertFailsWith<RecorderHandleException> { recorder.setOutputFile(newOutputFile("post-release.flac")) }
		assertFailsWith<RecorderHandleException> { recorder.prepare() }
		assertFailsWith<RecorderHandleException> { recorder.start() }
		assertFailsWith<RecorderHandleException> { recorder.stop() }

		val file = newOutputFile("recreated.flac")

		recorder.create()
		recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
		recorder.setOutputFile(file)
		recorder.prepare()
		recorder.start()

		delay(RECORD_DURATION_MS)

		recorder.stop()

		assertReadableFlac(file)
	}

	private fun assertReadableFlac(file: File, channelCount: Int = 1) {
		val output = TestUtil.extractAllSamplesFromFilePath(FlacExtractor(), file.absolutePath)

		assertThat(output.numberOfTracks).isEqualTo(1)

		val seekMap = output.seekMap
		assertThat(seekMap).isNotNull()
		assertThat(seekMap.durationUs).isGreaterThanOrEqualTo(MINIMUM_EXPECTED_DURATION_US)

		val track = output.trackOutputs.valueAt(0)
		val format = track.lastFormat

		assertThat(format?.sampleRate).isNotNull().isGreaterThan(0)
		assertThat(channelCount).isEqualTo(format?.channelCount)
		assertThat(C.ENCODING_PCM_16BIT).isEqualTo(format?.pcmEncoding)

		// The recording ran for RECORD_DURATION_MS, so the file must hold a
		// meaningful amount of audio, not just a few stray frames.
		//
		// media3's FakeTrackOutput.sampleCount counts extractor-emitted FLAC
		// frames (one per ~4096 samples), not audio samples, so assert on the
		// file's own STREAMINFO total-samples field instead.
		val sampleRate = requireNotNull(format?.sampleRate)
		assertStructurallyValidFlac(
			file,
			sampleRate = sampleRate,
			channelCount = channelCount,
			minimumTotalSamples = sampleRate / 4L,
		)
	}

	// Validates the FLAC container structure directly: the "fLaC" magic, the
	// STREAMINFO metadata block, and the stream description it declares,
	// including the total sample count libFLAC rewrites at finalize. This
	// works even when the file holds zero audio frames, which media3's
	// FlacExtractor cannot parse.
	private fun assertStructurallyValidFlac(
		file: File,
		sampleRate: Int,
		channelCount: Int,
		minimumTotalSamples: Long = 0L,
	) {
		val bytes = file.readBytes()

		// magic (4) + metadata block header (4) + STREAMINFO (34)
		assertThat(bytes.size).isGreaterThanOrEqualTo(42)
		assertThat(String(bytes, 0, 4, Charsets.US_ASCII)).isEqualTo("fLaC")

		val blockHeader = bytes[4].toInt() and 0xFF
		assertThat(blockHeader and 0x7F).isEqualTo(0) // STREAMINFO

		val blockSize = ((bytes[5].toInt() and 0xFF) shl 16) or
				((bytes[6].toInt() and 0xFF) shl 8) or
				(bytes[7].toInt() and 0xFF)
		assertThat(blockSize).isEqualTo(34)

		// STREAMINFO packs the stream description after 10 bytes of block and
		// frame size bounds: a 20-bit sample rate, a 3-bit channel count, a
		// 5-bit bit depth (all minus one) and a 36-bit total sample count.
		val declaredSampleRate =
			((bytes[18].toInt() and 0xFF) shl 12) or
					((bytes[19].toInt() and 0xFF) shl 4) or
					((bytes[20].toInt() and 0xF0) shr 4)
		val declaredChannels = ((bytes[20].toInt() shr 1) and 0x07) + 1
		val declaredBitsPerSample =
			(((bytes[20].toInt() and 0x01) shl 4) or ((bytes[21].toInt() and 0xF0) shr 4)) + 1

		// The 36-bit total sample count straddles bytes 21-25: the low nibble
		// of byte 21 holds its top 4 bits.
		val declaredTotalSamples =
			((bytes[21].toLong() and 0x0F) shl 32) or
					((bytes[22].toLong() and 0xFF) shl 24) or
					((bytes[23].toLong() and 0xFF) shl 16) or
					((bytes[24].toLong() and 0xFF) shl 8) or
					(bytes[25].toLong() and 0xFF)

		assertThat(declaredSampleRate).isEqualTo(sampleRate)
		assertThat(declaredChannels).isEqualTo(channelCount)
		assertThat(declaredBitsPerSample).isEqualTo(16)
		assertThat(declaredTotalSamples).isGreaterThanOrEqualTo(minimumTotalSamples)
	}

	private fun newOutputFile(name: String): File = File(outputFolder, name)

	companion object {

		private const val SAMPLE_RATE = 44_100
		private val RECORD_DURATION_MS = 1_500.milliseconds

		// Recordings asserted through assertReadableFlac ran for at least
		// RECORD_DURATION_MS, so anything shorter than a quarter second
		// means the pipeline dropped audio or finalized too early.
		private const val MINIMUM_EXPECTED_DURATION_US = 250_000L
	}
}
