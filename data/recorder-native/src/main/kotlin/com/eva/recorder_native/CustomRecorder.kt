package com.eva.recorder_native

import com.eva.recorder_native.exception.InvalidHandleResultException
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import java.io.File

/**
 * Lifecycle contract shared by the native recorders of this module,
 * [WavAudioRecorder] and [FlacAudioRecorder].
 *
 * All audio capture, buffering, and file encoding happens in native code behind
 * a JNI handle; implementing classes are thin Kotlin wrappers that forward each
 * call to the matching native entry point. Both implementations expose the same
 * behaviour, only the produced file format differs.
 *
 * ## Call order
 *
 * ```
 *  create()  ── required once, before anything else
 *     │
 *     ├─ setRecorderConfig()   optional, defaults to 44_100 Hz mono
 *     ├─ setOutputFile()       required before prepare()
 *     │
 *     ▼
 *  prepare()  ── opens the oboe input stream and the output file,
 *     │           locks the config and the path, is retryable on failure
 *     ▼
 *  start()  ──► RECORDING
 *                 │
 *                 ├─ pause() ──► paused ──► resume() ──► RECORDING
 *                 │
 *                 └─ stop() ──► FINALIZED, the file is complete and closed
 *                                  │
 *                                  ├─ start() throws here, call reset() first
 *                                  │
 *                                  └─ reset() ──► CREATED, path and config
 *                                                 cleared, record to a new file
 *
 *  release()  ── frees the native recorder from any state, is idempotent,
 *                and turns every other call into RecorderHandleException
 *
 *  stop() before prepare() is a silent no-op
 * ```
 *
 * ## Error contract
 *
 * Every method except [release] throws
 * [com.eva.recorder_native.exception.RecorderHandleException] when called
 * before [create] or after [release]. Beyond that guard:
 *
 *  - [create] throws
 *    [RecorderAlreadyInitiatedException]
 *    (an [IllegalStateException]) on a second call, and
 *    [InvalidHandleResultException] when the
 *    native recorder cannot be allocated.
 *  - [IllegalArgumentException] reports invalid values, such as a
 *    [RecorderConfig] with a non positive sample rate or channel count, or an
 *    empty output path. It is the translation of the native
 *    `std::invalid_argument`.
 *  - [IllegalStateException] reports lifecycle misuse, such as configuring
 *    after [prepare], starting before [prepare], starting twice, pausing while
 *    not recording, or resuming after the file was finalized. It is the
 *    translation of the native `std::logic_error`.
 *  - [RuntimeException] reports environment failures, such as an audio stream
 *    that could not be opened or started, or a file that could not be written
 *    or finalized. It is the translation of the remaining native
 *    `std::exception` family.
 */
interface CustomRecorder {

	/**
	 * Allocates the native recorder behind this instance. Must be the first
	 * call in the lifecycle; a second [create] is rejected until [release]
	 * frees the recorder.
	 *
	 * @throws RecorderAlreadyInitiatedException if this recorder is already created.
	 * @throws InvalidHandleResultException if the native recorder could not be allocated.
	 */
	fun create()

	/**
	 * Frees the native recorder and any recording resources it still holds.
	 * Valid from every state and idempotent: releasing a recorder that was
	 * never created, or was already released, is a no-op. Every other method
	 * throws [RecorderHandleException]
	 * afterwards, until [create] is called again.
	 *
	 * @throws RuntimeException if native teardown fails, for example when a
	 *         background recording error surfaces during cleanup.
	 */
	fun release()

	/**
	 * True while a recording is in progress, between [start] and [stop] or
	 * [pause].
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 */
	val isRecording: Boolean

	/**
	 * Sets the path the next recording will be written to. Must be called
	 * before [prepare]; once prepared, the path is locked until [reset] or
	 * [release]. An existing file at the path is truncated when [prepare]
	 * opens it.
	 *
	 * @param file target file of the next recording.
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalArgumentException if the path is empty.
	 * @throws IllegalStateException after [prepare] has locked the output file.
	 */
	fun setOutputFile(file: File)

	/**
	 * Configures the capture parameters of the next recording. Optional: the
	 * default [RecorderConfig] is 44_100 Hz mono. Must be called before
	 * [prepare]; once prepared, the config is locked until [reset] or
	 * [release].
	 *
	 * [prepare] may adapt these values to what the device actually delivers,
	 * and the written file always declares the real stream parameters.
	 *
	 * @param config capture parameters for the next recording.
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalArgumentException if [RecorderConfig.sampleRate] or
	 *         [RecorderConfig.channel] is not positive.
	 * @throws IllegalStateException after [prepare] has locked the config.
	 */
	fun setRecorderConfig(config: RecorderConfig = RecorderConfig())

	/**
	 * Opens the oboe input stream and the output file, and locks both. An
	 * output file must have been set with [setOutputFile]. A failed
	 * [prepare] leaves the recorder usable: after the cause is fixed,
	 * setting a new path or config and preparing again succeeds.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalStateException if already prepared, or if no output
	 *         file was configured.
	 * @throws RuntimeException if the audio stream or the output file could
	 *         not be opened.
	 */
	fun prepare()

	/**
	 * Begins capturing audio into the prepared output file. Capture runs on
	 * a native callback thread and buffered audio is drained to the file by
	 * a native worker thread.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalStateException if the recorder was not prepared, is
	 *         already recording, or the previous recording was finalized by
	 *         [stop] (call [reset] first).
	 * @throws RuntimeException if the audio stream failed to start, or if a
	 *         background recording error recorded earlier surfaces here.
	 */
	fun start()

	/**
	 * Stops capturing, drains the buffered audio, and finalizes the file.
	 * Calling [stop] again, or calling it on a created but never prepared
	 * recorder, is a no-op.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws RuntimeException if draining or finalizing the file failed, or
	 *         if a background recording error surfaces here.
	 */
	fun stop()

	/**
	 * Pauses the recording: captured audio is discarded while the recorder
	 * is paused.
	 *
	 * Note: the current native implementation is flag-based. Pausing lets the
	 * audio stream wind down, and [resume] does not yet restart it, so audio
	 * captured after a resume does not currently reach the file. Stream
	 * restart on resume is work in progress.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalStateException if the recorder was not prepared or is
	 *         not currently recording.
	 */
	fun pause()

	/**
	 * Resumes a recording paused with [pause].
	 *
	 * Note: see [pause]; the current native implementation re-arms the
	 * recording state without restarting the stream yet.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws IllegalStateException if the recorder was not prepared, is
	 *         already recording, or the recording was already finalized.
	 */
	fun resume()

	/**
	 * Finalizes any open recording, clears the output path and the config,
	 * and returns the recorder to the created state, ready to record into a
	 * new file. This is the way to record again after [stop] finalized a
	 * file, other than releasing and creating a new recorder.
	 *
	 * @throws RecorderHandleException before [create] or after [release].
	 * @throws RuntimeException if finalizing the previous recording failed.
	 */
	fun reset()
}
