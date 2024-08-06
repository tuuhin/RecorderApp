package com.eva.recorderapp.voice_recorder.data.player

import android.util.Log
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER = "AUDIO_FILE_PLAYER"

class AudioFilePlayerImpl(
	private val player: Player
) : AudioFilePlayer {

	private val lock = Mutex()

	private val _playerState = MutableStateFlow(PlayerState.IDLE)
	private val _playBackSpeed = MutableStateFlow(PlayerPlayBackSpeed.NORMAL)
	private val _isLooping = MutableStateFlow(false)

	private val _listener = object : Player.Listener {

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			val state = if (isPlaying) PlayerState.PLAYING
			else PlayerState.PAUSED
			// only updates to play or pause mode
			_playerState.update { state }
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			super.onPlaybackStateChanged(playbackState)
			val newState = when (playbackState) {
				Player.STATE_IDLE -> PlayerState.IDLE
				Player.STATE_ENDED -> PlayerState.COMPLETED
				Player.STATE_READY -> PlayerState.PLAYER_READY
				else -> null
			} ?: return
			// only updates idle ready and ended
			_playerState.update { newState }
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			val isLooping = repeatMode == Player.REPEAT_MODE_ONE
			// is current one looping
			_isLooping.update { isLooping }
		}

		override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
			val paramsSpeed = playbackParameters.speed
			val speed = PlayerPlayBackSpeed.fromInt(paramsSpeed) ?: return
			// updates the player speed
			_playBackSpeed.update { speed }
		}

		override fun onPlayerError(error: PlaybackException) {
			// need to check for exceptions
			Log.e(LOGGER, error.message ?: "PLAYER_ERROR", error)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _playerState.flatMapLatest(::computeMusicTrackInfo)

	override val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = combine(_playerState, _isLooping, _playBackSpeed) { state, repeat, speed ->
			PlayerMetaData(
				playerState = state,
				isRepeating = repeat,
				playBackSpeed = speed
			)
		}

	fun computeMusicTrackInfo(state: PlayerState): Flow<PlayerTrackData> {
		return flow<PlayerTrackData> {
			Log.d(LOGGER, "PLAYER STATE: $state")

			if (state == PlayerState.PLAYER_READY) {
				val duration = PlayerTrackData(
					current = player.currentPosition.milliseconds,
					total = player.duration.milliseconds,
				)
				emit(duration)
			}
			// if the player is playing or paused the track info sshould be updated
			while (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
				val duration = PlayerTrackData(
					current = player.currentPosition.milliseconds,
					total = player.duration.milliseconds,
				)
				emit(duration)
				delay(100.milliseconds)
			}
		}.distinctUntilChanged()
	}

	override fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) {
		val command = player.availableCommands.contains(Player.COMMAND_SET_SPEED_AND_PITCH)
		if (!command) {
			Log.d(LOGGER, "CANNOT CHANGE SPEED COMMAND NOT AVAILABLE")
			return
		}
		player.setPlaybackSpeed(playBackSpeed.speed)
	}

	override fun setPlayLooping(loop: Boolean) {
		val command = player.availableCommands.contains(Player.COMMAND_SET_REPEAT_MODE)
		if (!command) {
			Log.d(LOGGER, "CANNOT REPEAT MODE COMMAND NOT AVAILABLE")
			return
		}
		val repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
		player.repeatMode = repeatMode
	}

	override fun onMuteDevice() {
		val command = player.availableCommands
			.contains(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)

		if (!command) {
			Log.d(LOGGER, "PLAYER COMMAND NOT FOUND")
			return
		}
//		player.setDeviceMuted(true, C.VOLUME_FLAG_REMOVE_SOUND_AND_VIBRATE)
	}

	override suspend fun preparePlayer(audio: AudioFileModel): Resource<Boolean, Exception> {
		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "METHOD IS LOCKED")
			return Resource.Success(false)
		}
		//  locking this w.r.t to this class
		lock.lock(this)

		try {
			val mediaItem = prepareMediaItem(audio)

			Log.i(LOGGER, "CREATING MEDIA ITEM FOR ITEM : ${audio.id}")
			player.apply {
				prepare()
				setMediaItem(mediaItem)
				addListener(_listener)
			}
			Log.d(LOGGER, "PLAYER PREPARED AND MEDIA ITEM SET")
			return Resource.Success(true)
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			Log.e(LOGGER, "PLAYER IS NOT CONFIGURED")
			return Resource.Error(e)
		} catch (e: Exception) {
			e.printStackTrace()
			return Resource.Error(e)
		} finally {
			lock.unlock(this)
		}
	}


	override suspend fun pausePlayer() {
		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "OTHER FUNCTION IS HOLDING LOCK CANNOT PERFORM OPERATION")
			return
		}
		lock.lock(this)
		try {
			player.pause()
			Log.d(LOGGER, "PLAYER PAUSED")
		} catch (e: IllegalStateException) {
			Log.e(LOGGER, "PLAYER IS NOT CONFIGURED")
		} finally {
			lock.unlock(this)
		}
	}

	override suspend fun startOrResumePlayer() {
		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "OTHER FUNCTION IS HOLDING LOCK CANNOT PERFORM OPERATION")
			return
		}
		lock.lock(this)
		try {
			player.play()
			Log.d(LOGGER, "PLAYER RESUMED")
		} catch (e: IllegalStateException) {
			Log.e(LOGGER, "PLAYER IS NOT CONFIGURED")
		} finally {
			lock.unlock(this)
		}
	}

	override suspend fun stopPlayer() {
		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "CANNOT STOP PLAYER ITS LOCKED")
			return
		}
		//  locking this w.r.t to this class
		lock.lock(this)
		try {
			player.stop()
			Log.d(LOGGER, "PLAYER STOPPED AND RESET")
		} catch (e: IllegalStateException) {
			Log.d(LOGGER, "PLAYER MAY NOT BE CONFIGURED")
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			lock.unlock(this)
		}

	}

	override fun onSeekDuration(duration: Duration) {
		val totalDuration = player.duration
		val changedDuration = duration.inWholeMilliseconds
		if (changedDuration <= totalDuration) {
			player.seekTo(duration.inWholeMilliseconds)
		}
	}

	override fun forwardPlayerByNDuration(duration: Duration) {
		val seekPosition = player.currentPosition + duration.inWholeMilliseconds
		if (player.currentMediaItem == null) {
			Log.d(LOGGER, "PLAYER MEDIA ITEM NOT SET")
			return
		} else if (seekPosition >= player.duration) {
			// seek to max duration
			Log.d(LOGGER, "SEEK POSITION IS PLAYER DURATION")
			player.seekTo(player.duration)
		}
		Log.d(LOGGER, "PLAYER POSITION CHANGED $seekPosition")
		player.seekTo(seekPosition)
	}

	override fun rewindPlayerByNDuration(duration: Duration) {
		val seekPosition = player.currentPosition - duration.inWholeMilliseconds
		if (player.currentMediaItem == null) {
			Log.d(LOGGER, "PLAYER MEDIA ITEM NOT SET")
			return
		} else if (seekPosition < 0) {
			Log.d(LOGGER, "SEEK POSITION IS LESSER THAN 0")
			player.seekTo(0)
		}
		Log.d(LOGGER, "PLAYER POSITION CHANGED $seekPosition")
		player.seekTo(seekPosition)
	}

	override fun clearResources() {
		player.removeListener(_listener)
		Log.d(LOGGER, "REMOVED LISTENER FOR PLAYER")
	}

	private fun prepareMediaItem(audio: AudioFileModel): MediaItem {
		val extrasBundle = bundleOf("AUDIO_FILE_ID" to audio.id)

		val metaData = MediaMetadata.Builder()
			.setTitle(audio.title)
			.setDisplayTitle(audio.displayName)
			.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
			.setRecordingDay(audio.lastModified.dayOfMonth)
			.setRecordingMonth(audio.lastModified.monthNumber)
			.setRecordingYear(audio.lastModified.year)
			.setIsBrowsable(false)
			.setExtras(extrasBundle)
			.build()

		val fileUri = audio.fileUri.toUri()
		return MediaItem.Builder()
			.setUri(fileUri)
			.setMediaId("${audio.id}")
			.setMimeType(audio.mimeType)
			.setMediaMetadata(metaData)
			.build()
	}
}