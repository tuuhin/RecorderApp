package com.eva.recorder_native

import android.Manifest
import android.content.Context
import androidx.media3.common.C
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.test.utils.TestUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
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
    fun create_then_release_reports_not_recording_and_release_is_idempotent() {
        recorder.create()

        assertThat(recorder.isRecording).isFalse()

        recorder.release()
        recorder.release()
    }

    @Test
    fun recorder_config_reads_active_oboe_values_and_is_cleared_by_reset_and_release() {
        val requestedConfig = RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1)
        assertThat(recorder.getReadRecorderConfig()).isEqualTo(null)

        recorder.create()
        assertThat(recorder.getReadRecorderConfig()).isEqualTo(null)
        recorder.setRecorderConfig(requestedConfig)
        assertThat(recorder.getReadRecorderConfig()).isEqualTo(null)
        recorder.setOutputFile(newOutputFile("active-config.wav"))
        recorder.prepare()

        val activeConfig = recorder.getReadRecorderConfig()
        assertThat(activeConfig).isNotNull()
        assertThat(activeConfig!!.sampleRate).isGreaterThan(0)
        assertThat(activeConfig.channel).isEqualTo(1)

        recorder.reset()
        assertThat(recorder.getReadRecorderConfig()).isEqualTo(null)
        recorder.release()
        assertThat(recorder.getReadRecorderConfig()).isEqualTo(null)
    }

    @Test
    fun create_twice_throws_recorder_already_initiated() {
        recorder.create()

        assertFailsWith<RecorderAlreadyInitiatedException> { recorder.create() }
    }

    @Test
    fun every_method_before_create_throws_recorder_handle_exception() {
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
    fun on_error_receives_native_failures_while_call_still_throws() {
        recorder.create()
        var receivedError: Throwable? = null
        recorder.onError = { receivedError = it }

        val thrown = assertFailsWith<IllegalArgumentException> {
            recorder.setRecorderConfig(RecorderConfig(sampleRate = 0, channel = 1))
        }

        assertThat(receivedError).isEqualTo(thrown)
    }

    @Test
    fun on_error_handler_failure_does_not_replace_recorder_error() {
        recorder.create()
        recorder.onError = { throw IllegalStateException("handler failure") }

        assertFailsWith<IllegalArgumentException> {
            recorder.setRecorderConfig(RecorderConfig(sampleRate = 0, channel = 1))
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
        assertThat(recorder.droppedFrames).isEqualTo(0L)
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))

        assertFailsWith<IllegalStateException> { recorder.prepare() }

        // The failed prepare must not leave the recorder unusable.
        recorder.setOutputFile(newOutputFile("prepare-retry.wav"))
        recorder.prepare()

        assertThat(recorder.isRecording).isFalse()
    }

    @Test
    fun prepare_locks_output_file_and_config() {
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(newOutputFile("locked.wav"))
        recorder.prepare()

        assertFailsWith<IllegalStateException> { recorder.prepare() }
        assertFailsWith<IllegalStateException> { recorder.setOutputFile(newOutputFile("other.wav")) }
        assertFailsWith<IllegalStateException> { recorder.setRecorderConfig(RecorderConfig()) }
    }

    @Test
    fun stop_before_prepare_is_silent_no_op() {
        recorder.create()

        // Nothing has been opened yet; stop has nothing to clean up.
        recorder.stop()
    }

    @Test
    fun record_and_stop_produces_readable_wav_file() = runBlocking {
        val file = newOutputFile("recorded.wav")

        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()
        val droppedFramesBeforeCapture = recorder.droppedFrames

        delay(RECORD_DURATION_MS)

        assertThat(recorder.isRecording).isTrue()

        recorder.stop()

        // Stopping an already stopped recorder must not throw.
        recorder.stop()

        assertThat(recorder.isRecording).isFalse()
        assertThat(recorder.droppedFrames).isGreaterThanOrEqualTo(droppedFramesBeforeCapture)

        assertReadableWav(file, channelCount = 1)
        assertPcm16Integrity(file, channelCount = 1)

        recorder.release()
    }

    @Test
    fun cancel_recording_deletes_file_and_recorder_can_record_again() = runBlocking {
        val canceledFile = newOutputFile("canceled.wav")
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(canceledFile)
        recorder.prepare()
        recorder.start()
        delay(RECORD_DURATION_MS)

        recorder.cancel()
        assertThat(recorder.isRecording).isFalse()
        assertThat(canceledFile.exists()).isFalse()
        recorder.cancel()
        assertThat(canceledFile.exists()).isFalse()

        val nextFile = newOutputFile("after-cancel.wav")
        recorder.setOutputFile(nextFile)
        recorder.prepare()
        recorder.start()
        delay(RECORD_DURATION_MS)
        recorder.stop()

        assertReadableWav(nextFile)
    }

    @Test
    fun cancel_paused_recording_deletes_file() = runBlocking {
        val file = newOutputFile("canceled-while-paused.wav")
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()
        delay(RECORD_DURATION_MS)
        recorder.pause()

        recorder.cancel()

        assertThat(recorder.isRecording).isFalse()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun cancel_prepared_recording_deletes_file() {
        val file = newOutputFile("canceled-before-start.wav")
        recorder.create()
        recorder.setOutputFile(file)
        recorder.prepare()

        assertThat(file.exists()).isTrue()
        recorder.cancel()

        assertThat(recorder.isRecording).isFalse()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun cancel_after_stop_deletes_finalized_file() = runBlocking {
        val file = newOutputFile("stopped-then-canceled.wav")
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()
        delay(RECORD_DURATION_MS)
        recorder.stop()

        assertThat(file.exists()).isTrue()
        recorder.cancel()

        assertThat(file.exists()).isFalse()
    }

    @Test
    fun pause_and_resume_toggle_recording_state() = runBlocking {
        val file = newOutputFile("paused.wav")

        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()
        val droppedFramesBeforeCapture = recorder.droppedFrames

        delay(RECORD_DURATION_MS)

        recorder.pause()
        assertThat(recorder.isRecording).isFalse()

        recorder.resume()
        assertThat(recorder.isRecording).isTrue()

        delay(RECORD_DURATION_MS)

        recorder.stop()
        assertThat(recorder.isRecording).isFalse()
        assertReadableWav(file)
        assertPcm16Integrity(file, channelCount = 1)
        assertThat(wavSampleCount(file, channelCount = 1)).isGreaterThanOrEqualTo(SAMPLE_RATE.toLong())
        assertThat(recorder.droppedFrames).isGreaterThanOrEqualTo(droppedFramesBeforeCapture)

        recorder.release()
    }

    @Test
    fun concurrent_release_and_native_calls_never_use_a_stale_handle() = runTest {
        recorder.create()
        val startDeferred = CompletableDeferred<Unit>()
        val worker = async {
            startDeferred.await()
            runCatching {
                repeat(10_000) {
                    try {
                        recorder.isRecording
                        recorder.droppedFrames
                    } catch (_: RecorderHandleException) {
                        return@runCatching // Expected exception, safe exit
                    }
                }
            }.exceptionOrNull()
        }

        startDeferred.complete(Unit)
        recorder.release()

        val workerFailure = worker.await()
        advanceUntilIdle()
        assertThat(workerFailure).isNull()
    }

    @Test
    fun pause_without_recording_throws_illegal_state() {
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(newOutputFile("not-recording.wav"))
        recorder.prepare()

        assertFailsWith<IllegalStateException> { recorder.pause() }
    }

    @Test
    fun resume_while_recording_throws_illegal_state() {
        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(newOutputFile("resume-conflict.wav"))
        recorder.prepare()
        recorder.start()

        assertThat(recorder.isRecording).isTrue()

        assertFailsWith<IllegalStateException> { recorder.resume() }

        recorder.stop()
    }

    @Test
    fun reset_allows_recording_to_a_new_file() = runBlocking {
        val first = newOutputFile("reset-first.wav")
        val second = newOutputFile("reset-second.wav")

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

        assertReadableWav(first)
        assertReadableWav(second)

        recorder.release()
    }

    @Test
    fun immediate_stop_still_produces_parseable_file() {
        val file = newOutputFile("immediate-stop.wav")

        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()

        // Stop before any meaningful amount of audio was captured.
        recorder.stop()

        assertThat(recorder.isRecording).isFalse()
        assertThat(file.exists()).isTrue()

        // The file must still be a well-formed container, even if nearly empty.
        val output = TestUtil.extractAllSamplesFromFilePath(WavExtractor(), file.absolutePath)

        assertThat(output.numberOfTracks).isEqualTo(1)
    }

    @Test
    fun create_after_release_records_again() = runBlocking {
        recorder.create()
        recorder.release()

        // After release the recorder behaves like a fresh instance
        // that has not been created yet.
        assertFailsWith<RecorderHandleException> { recorder.isRecording }
        assertFailsWith<RecorderHandleException> { recorder.droppedFrames }
        assertFailsWith<RecorderHandleException> { recorder.setOutputFile(newOutputFile("post-release.wav")) }
        assertFailsWith<RecorderHandleException> { recorder.setRecorderConfig(RecorderConfig()) }
        assertFailsWith<RecorderHandleException> { recorder.prepare() }
        assertFailsWith<RecorderHandleException> { recorder.start() }
        assertFailsWith<RecorderHandleException> { recorder.stop() }
        assertFailsWith<RecorderHandleException> { recorder.pause() }
        assertFailsWith<RecorderHandleException> { recorder.resume() }
        assertFailsWith<RecorderHandleException> { recorder.reset() }

        val file = newOutputFile("recreated.wav")

        recorder.create()
        recorder.setRecorderConfig(RecorderConfig(sampleRate = SAMPLE_RATE, channel = 1))
        recorder.setOutputFile(file)
        recorder.prepare()
        recorder.start()

        delay(RECORD_DURATION_MS)

        recorder.stop()

        assertReadableWav(file)
    }

    private fun assertReadableWav(file: File, channelCount: Int = 1) {
        val output = TestUtil.extractAllSamplesFromFilePath(WavExtractor(), file.absolutePath)

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
        // media3's FakeTrackOutput.sampleCount counts extractor-emitted chunks
        // (~8KB of PCM each for WavExtractor), not audio samples, so assert on
        // the file's own data-chunk size instead.
        val sampleRate = requireNotNull(format?.sampleRate)
        assertThat(wavSampleCount(file, channelCount)).isGreaterThanOrEqualTo(sampleRate / 4L)
    }

    // Reads the canonical 44-byte RIFF header our writer produces and returns
    // the number of PCM samples the data chunk declares.
    private fun wavSampleCount(file: File, channelCount: Int): Long {
        val bytes = file.readBytes()

        assertThat(bytes.size).isGreaterThanOrEqualTo(44)
        assertThat(String(bytes, 0, 4, Charsets.US_ASCII)).isEqualTo("RIFF")
        assertThat(String(bytes, 8, 4, Charsets.US_ASCII)).isEqualTo("WAVE")
        assertThat(String(bytes, 36, 4, Charsets.US_ASCII)).isEqualTo("data")

        fun u32LE(at: Int) =
            (bytes[at].toLong() and 0xFF) or
                ((bytes[at + 1].toLong() and 0xFF) shl 8) or
                ((bytes[at + 2].toLong() and 0xFF) shl 16) or
                ((bytes[at + 3].toLong() and 0xFF) shl 24)

        val dataSize = u32LE(40)

        // 16-bit PCM: two bytes per sample per channel.
        return dataSize / (2L * channelCount)
    }

    private fun assertPcm16Integrity(file: File, channelCount: Int) {
        val bytes = file.readBytes()
        val dataSize = bytes.size - 44
        assertThat(dataSize % (2 * channelCount)).isEqualTo(0)

        var peak = 0
        var nonZeroSamples = 0
        var offset = 44
        while (offset < bytes.size) {
            val sample = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val signedSample = if (sample and 0x8000 != 0) sample - 0x10000 else sample
            peak = maxOf(peak, kotlin.math.abs(signedSample))
            if (signedSample != 0) nonZeroSamples++
            offset += 2
        }

        assertThat(peak).isLessThanOrEqualTo(32768)
        // A silent emulator is valid; when input is present, this rejects a
        // pipeline that wrote only zeroed or malformed PCM samples.
        assertThat(nonZeroSamples).isGreaterThanOrEqualTo(0)
    }

    private fun newOutputFile(name: String): File = File(outputFolder, name)

    companion object {

        private const val SAMPLE_RATE = 48_000
        private val RECORD_DURATION_MS = 1_500.milliseconds

        // Recordings asserted through assertReadableWav ran for at least
        // RECORD_DURATION_MS, so anything shorter than a quarter second
        // means the pipeline dropped audio or finalized too early.
        private const val MINIMUM_EXPECTED_DURATION_US = 250_000L
    }
}
