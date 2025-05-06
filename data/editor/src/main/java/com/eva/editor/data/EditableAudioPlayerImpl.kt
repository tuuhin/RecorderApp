package com.eva.editor.data

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.exceptions.AudioClipException
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player.data.util.computeIsPlayerPlaying
import com.eva.player.data.util.computePlayerTrackData
import com.eva.player.data.util.isMediaItemChange
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "EDITABLE_ITEM_PLAYER"

internal class EditableAudioPlayerImpl(
	private val player: Player,
	private val sourceFactory: MediaSource.Factory
) : SimpleAudioPlayer {

	constructor(player: Player, context: Context) : this(player, DefaultMediaSourceFactory(context))

	private val _lock = Mutex()

	override val isPlaying: Flow<Boolean>
		get() = player.computeIsPlayerPlaying()

	override val isMediaItemChanged: Flow<Boolean>
		get() = player.isMediaItemChange()

	@OptIn(ExperimentalCoroutinesApi::class)
	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = player.computePlayerTrackData()

	override fun onSeekDuration(duration: Duration) {
		val command = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
		if (!command) {
			Log.w(TAG, "PLAYER SEEK IN MEDIA COMMAND NOT FOUND")
			return
		}
		val totalDuration = player.duration
		val changedDuration = duration.inWholeMilliseconds
		if (changedDuration <= totalDuration) {
			Log.d(TAG, "SEEK POSITION $duration")
			player.seekTo(duration.inWholeMilliseconds)
		}
	}

	override suspend fun prepareAudioFile(audio: AudioFileModel) {
		val command = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)
		if (!command) return

		return _lock.checkLockAndPerformOperation(
			action = {
				// add media item
				val mediaItem = MediaItem.fromUri(audio.fileUri)
				player.setMediaItem(mediaItem)

				if (player.playbackState == Player.STATE_IDLE) {
					player.prepare()
					Log.d(TAG, "PLAYER PREPARED AND READY TO PLAY AUDIO")
					player.playWhenReady = false
				}
			},
		)
	}

	override suspend fun cropMediaPortion(audio: AudioFileModel, config: AudioClipConfig)
			: Result<Unit> {
		if (!config.validate(audio.duration))
			return Result.failure(AudioClipException())

		return _lock.runOtherwiseCancelIfLocked(
			action = {
				val mediaItem = player.currentMediaItem ?: MediaItem.fromUri(audio.fileUri)
					.buildUpon()
					.setMediaId("${audio.id}")
					.build()

				val clippingConfig = MediaItem.ClippingConfiguration.Builder()
					.setStartPositionMs(config.start.inWholeMilliseconds)
					.setEndPositionMs(config.end.inWholeMilliseconds)
					.build()

				val metaData = mediaItem.mediaMetadata.buildUpon()
					.setDurationMs(config.end.inWholeMilliseconds - config.start.inWholeMilliseconds)
					.build()

				val clippedMediaItem = mediaItem.buildUpon()
					.setClippingConfiguration(clippingConfig)
					.setMediaMetadata(metaData)
					.build()
				Log.d(TAG, "CHANGING CURRENT MEDIA ITEM WITH CLIP CONFIG : $config")
				// set the new clipped media and start position to 0
				player.setMediaItem(clippedMediaItem, 0L)
			},
		)
	}

	@UnstableApi
	override suspend fun cutMediaPortion(audio: AudioFileModel, config: AudioClipConfig)
			: Result<Unit> {
		if (!config.validate(audio.duration))
			return Result.failure(AudioClipException())

		return _lock.runOtherwiseCancelIfLocked(
			action = {
				val mediaItem = player.currentMediaItem ?: MediaItem.fromUri(audio.fileUri)
					.buildUpon()
					.setMediaId("${audio.id}")
					.build()

				val totalDurationInMs = mediaItem.mediaMetadata.durationMs
					?: audio.duration.inWholeMilliseconds

				val firstPartClip = mediaItem.buildUpon()
					.setClippingConfiguration(
						MediaItem.ClippingConfiguration.Builder()
							.setStartPositionMs(0)
							.setEndPositionMs(config.start.inWholeMilliseconds)
							.build()
					).build()

				val secondClip = mediaItem.buildUpon()
					.setClippingConfiguration(
						MediaItem.ClippingConfiguration.Builder()
							.setStartPositionMs(config.end.inWholeMilliseconds)
							.setEndPositionMs(totalDurationInMs)
							.build()
					).build()

				val concatItemDuration = firstPartClip.clippingConfiguration.clipDuration +
						secondClip.clippingConfiguration.clipDuration

				Log.d(TAG, "CONCAT ITEM DURATION :$concatItemDuration")

				val concatMetaData = mediaItem.mediaMetadata.buildUpon()
					.setDurationMs(concatItemDuration.inWholeMilliseconds).build()

				val concatSources = ConcatenatingMediaSource2.Builder()
					.setMediaItem(mediaItem.buildUpon().setMediaMetadata(concatMetaData).build())
					.setMediaSourceFactory(sourceFactory)
					.add(firstPartClip)
					.add(secondClip)
					.build()

				Log.d(TAG, "CHANGING CURRENT MEDIA ITEM WITH CUT :${concatSources.mediaItem}")
				// only allow this if this is a exoplayer instance
				(player as? ExoPlayer)?.setMediaSource(concatSources, 0L)
			},
		)
	}

	override suspend fun pausePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		return _lock.checkLockAndPerformOperation(
			action = {
				player.pause()
				Log.d(TAG, "PLAYER PAUSED")
			},
		)
	}

	override suspend fun startOrResumePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		return _lock.checkLockAndPerformOperation(
			action = {
				player.play()
				Log.d(TAG, "PLAYER RESUMED")
			},
		)
	}

	override suspend fun stopPlayer() {
		return _lock.checkLockAndPerformOperation(
			action = {
				player.stop()
				Log.d(TAG, "PLAYER STOPPED AND RESET")
			},
		)
	}

	override fun cleanUp() {
		player.clearMediaItems()
	}

	private suspend inline fun Mutex.checkLockAndPerformOperation(
		action: () -> Unit,
		onError: (Exception) -> Unit = {},
	) {
		if (holdsLock(this)) {
			Log.d(TAG, "CANNOT PERFORM OPERATION")
			return
		}
		withLock {
			try {
				action()
			} catch (e: Exception) {
				Log.e(TAG, "SOME ERROR", e)
				onError(e)
			}
		}
	}

	private suspend inline fun Mutex.runOtherwiseCancelIfLocked(
		action: () -> Unit,
		onError: (Exception) -> Unit = {},
	): Result<Unit> {
		if (holdsLock(this)) {
			Log.d(TAG, "CANNOT PERFORM OPERATION")
			return Result.failure(Exception("Cannot perform operation"))
		}
		return withLock {
			try {
				action()
				Result.success(Unit)
			} catch (e: Exception) {
				Log.e(TAG, "SOME ERROR", e)
				onError(e)
				Result.failure(e)
			}
		}
	}

	private val MediaItem.ClippingConfiguration.clipDuration: Duration
		get() {
			val end = endPositionMs.milliseconds
			val start = startPositionMs.milliseconds
			val diff = end - start
			return if (diff.inWholeMicroseconds > 0L) diff
			else 0.milliseconds
		}
}