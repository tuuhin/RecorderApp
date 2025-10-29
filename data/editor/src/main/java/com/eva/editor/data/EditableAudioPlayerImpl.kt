package com.eva.editor.data

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.AudioConfigsList
import com.eva.editor.domain.EditorComposer
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.exceptions.AudioClipException
import com.eva.editor.domain.exceptions.InvalidPlayerException
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player.data.util.computeIsPlayerPlaying
import com.eva.player.data.util.computePlayerTrackData
import com.eva.player.data.util.isMediaItemChange
import com.eva.player.di.MediaSourceFactory
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.tryWithLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "EDITOR_AUDIO_PLAYER"

internal class EditableAudioPlayerImpl(
	private val player: Player,
	private val sourceFactory: MediaSourceFactory,
) : SimpleAudioPlayer {

	private val _lock = Mutex()

	override val isPlaying: Flow<Boolean>
		get() = player.computeIsPlayerPlaying()

	override val isMediaItemChanged: Flow<Boolean>
		get() = player.isMediaItemChange()

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
		val commands = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM) &&
				player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!commands) {
			Log.i(TAG, "MISSING COMMANDS")
			return
		}

		_lock.tryWithLock(owner = this) {
			// add media item
			val mediaItem = MediaItem.fromUri(audio.fileUri)

			player.setMediaItem(mediaItem)

			if (player.playbackState == Player.STATE_IDLE) {
				player.prepare()
				Log.d(TAG, "PLAYER PREPARED AND READY TO PLAY AUDIO")
				player.playWhenReady = false
			}
		}
	}

	override suspend fun cropMediaPortion(audio: AudioFileModel, config: AudioClipConfig)
			: Result<Unit> {
		if (!config.validate(audio.duration))
			return Result.failure(AudioClipException())

		return _lock.tryWithLock(this) {
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

		}
	}

	@UnstableApi
	override suspend fun cutMediaPortion(audio: AudioFileModel, config: AudioClipConfig)
			: Result<Unit> {
		if (!config.validate(audio.duration))
			return Result.failure(AudioClipException())

		return _lock.tryWithLock(owner = this) {
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
				.setDurationMs(concatItemDuration.inWholeMilliseconds)
				.build()

			val concatMediaItem = mediaItem.buildUpon()
				.setMediaMetadata(concatMetaData)
				.build()

			val concatSources = ConcatenatingMediaSource2.Builder()
				.setMediaItem(concatMediaItem)
				.setMediaSourceFactory(sourceFactory)
				.add(firstPartClip)
				.add(secondClip)
				.build()

			Log.d(TAG, "CHANGING CURRENT MEDIA ITEM WITH CUT :${concatSources.mediaItem}")

			// finally play the concatenated source
			val exoPlayer = (player as? ExoPlayer)
				?: return Result.failure(InvalidPlayerException())
			exoPlayer.setMediaSource(concatSources, 0L)
		}
	}

	@UnstableApi
	override suspend fun editMediaPortions(
		audio: AudioFileModel,
		configs: AudioConfigToActionList
	): Result<Unit> {
		return _lock.tryWithLock(owner = this) {

			val composition = withContext(Dispatchers.Default) {
				EditorComposer.applyLogicalEditSequence(audio.duration, configs)
			}

			val mediaItem = player.currentMediaItem ?: MediaItem.fromUri(audio.fileUri)
				.buildUpon()
				.setMediaId("${audio.id}")
				.build()

			Log.d(TAG, "CLIPPING CONTENT")
			Log.d(TAG, composition.joinToString("|"))

			val totalDurationInMicroSeconds = composition.sumOf { config ->
				val diff = config.end - config.start
				val duration = if (diff.inWholeMicroseconds > 0L) diff
				else 0.milliseconds
				duration.inWholeMilliseconds
			}

			val concatMetaData = mediaItem.mediaMetadata
				.buildUpon()
				.setDurationMs(totalDurationInMicroSeconds)
				.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
				.build()

			val concatMediaItem = mediaItem.buildUpon()
				.setMediaMetadata(concatMetaData)
				.build()

			val concatSourcesBuilder = ConcatenatingMediaSource2.Builder()
				.setMediaItem(concatMediaItem)
				.setMediaSourceFactory(sourceFactory)

			val clippedMediaItems = composition.mapToMediaItem(mediaItem)
			clippedMediaItems.forEach { concatSourcesBuilder.add(it) }

			val finalSource = concatSourcesBuilder.build()

			val exoPlayer = (player as? ExoPlayer)
				?: return Result.failure(InvalidPlayerException())
			// finally play the concatenated source
			exoPlayer.setMediaSource(finalSource, 0L)
		}
	}

	override suspend fun pausePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		_lock.tryWithLock(this) {
			player.pause()
			Log.d(TAG, "PLAYER PAUSED")
		}
	}

	override suspend fun startOrResumePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		_lock.tryWithLock(owner = this) {
			player.play()
			Log.d(TAG, "PLAYER RESUMED")
		}
	}

	override suspend fun stopPlayer() {
		_lock.tryWithLock(owner = this) {
			player.stop()
			Log.d(TAG, "PLAYER STOPPED ")
		}
	}

	override fun cleanUp() {
		if (player.isCommandAvailable(Player.COMMAND_RELEASE)) {
			Log.d(TAG, "RELEASING THE PLAYER")
			player.release()
		}
		Log.d(TAG, "CLEARING MEDIA ITEMS")
		player.clearMediaItems()
	}

	private fun AudioConfigsList.mapToMediaItem(mediaItem: MediaItem): List<MediaItem> {
		return map { config ->

			val clipConfig = MediaItem.ClippingConfiguration.Builder()
				.setStartPositionMs(config.start.inWholeMilliseconds)
				.setEndPositionMs(config.end.inWholeMilliseconds)
				.build()

			mediaItem.buildUpon()
				.setClippingConfiguration(clipConfig)
				.build()
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