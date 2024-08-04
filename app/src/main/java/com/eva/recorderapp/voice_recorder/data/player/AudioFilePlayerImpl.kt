package com.eva.recorderapp.voice_recorder.data.player

import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER = "AUDIO_FILE_PLAYER"

class AudioFilePlayerImpl(
	private val player: Player
) : AudioFilePlayer {

	private val lock = Mutex()

	private val _isPlaying = MutableStateFlow(false)
	private val _state = MutableStateFlow(PlayerState.IDLE)
	private val _playBackSpeed = MutableStateFlow(PlayerPlayBackSpeed.NORMAL)
	private val _isLooping = MutableStateFlow(false)

	private val _listener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			_isPlaying.update { isPlaying }
			_state.update { PlayerState.PLAYING }
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			val newState = when (playbackState) {
				Player.STATE_IDLE -> PlayerState.IDLE
				Player.STATE_ENDED -> PlayerState.COMPLETED
				Player.STATE_READY -> PlayerState.READY
				Player.STATE_BUFFERING -> PlayerState.BUFFEREING
				else -> null
			}
			newState?.let { _state.update { it } }
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			val isLooping = when (repeatMode) {
				Player.REPEAT_MODE_ONE -> true
				else -> false
			}
			_isLooping.update { isLooping }
		}

		override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
			val paramsSpeed = playbackParameters.speed
			val speed = PlayerPlayBackSpeed.fromInt(paramsSpeed) ?: return
			_playBackSpeed.update { speed }
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _isPlaying.flatMapLatest(::computeMusicTrackInfo)

	override val isPlaying: StateFlow<Boolean>
		get() = _isPlaying.asStateFlow()

	override val playerState: StateFlow<PlayerState>
		get() = _state.asStateFlow()

	override val playBackSpeedFlow: StateFlow<PlayerPlayBackSpeed>
		get() = _playBackSpeed.asStateFlow()

	override val isLooping: StateFlow<Boolean>
		get() = _isLooping.asStateFlow()

	fun computeMusicTrackInfo(canCompute: Boolean): Flow<PlayerTrackData> {
		return flow {
			//configurations
			val isPlaying = player.playWhenReady && canCompute
			val isOk = player.availableCommands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)

			emit(PlayerTrackData(total = player.duration.milliseconds))

			while (isPlaying && isOk) {
				delay(200.milliseconds)
				val duration = PlayerTrackData(
					current = player.currentPosition.milliseconds,
					total = player.duration.milliseconds,
				)
				emit(duration)
			}
		}
	}

	override fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) {
		val isOk = player.availableCommands.contains(Player.COMMAND_SET_SPEED_AND_PITCH)
		if (!isOk) {
			Log.d(LOGGER, "CANNOT CHANGE SPEED COMMAND NOT AVAILABLE")
			return
		}
		player.setPlaybackSpeed(playBackSpeed.speed)
	}

	override fun setPlayLooping(loop: Boolean) {
		val isOk = player.availableCommands.contains(Player.COMMAND_SET_REPEAT_MODE)
		if (!isOk) {
			Log.d(LOGGER, "CANNOT REPEAT MODE COMMAND NOT AVAILABLE")
			return
		}
		val repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
		player.repeatMode = repeatMode
	}

	override fun onMuteDevice() {
		player.setDeviceMuted(true, C.VOLUME_FLAG_REMOVE_SOUND_AND_VIBRATE)
	}

	override suspend fun preparePlayer(audio: AudioFileModel): Resource<Boolean, Exception> {
		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "PREPARING THE PLAYER CANNOT CALL THIS UTIL THE OTHER ONE IS DONE")
			return Resource.Success(false)
		}
		//  locking this w.r.t to this class
		lock.lock(this)


		val metaData = MediaMetadata.Builder()
			.setTitle(audio.title)
			.setDisplayTitle(audio.displayName)
			.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
			.setRecordingDay(audio.lastModified.dayOfMonth)
			.setRecordingMonth(audio.lastModified.monthNumber)
			.setRecordingYear(audio.lastModified.year)
			.setIsBrowsable(false)
			.build()

		try {

			val fileUri = audio.fileUri.toUri()
			val mediaItem = MediaItem.Builder()
				.setUri(fileUri)
				.setMediaId("${audio.id}")
				.setMimeType(audio.mimeType)
				.setMediaMetadata(metaData)
				.build()

			Log.i(LOGGER, "CREATING MEDIA ITEM FOR ITEM : ${audio.id}")
			player.apply {
				prepare()
				setMediaItem(mediaItem)
				addListener(_listener)
			}
			Log.d(LOGGER, "PLAYER PREPARED READY TO PLAY")
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

	override fun forwardPlayerByNDuration(duration: Duration) {
		try {
			val seekPosition = player.currentPosition + duration.inWholeMilliseconds
			if (player.currentMediaItem == null) {
				Log.d(LOGGER, "PLAYER MEDIA ITEM NOT SET")
				return
			} else if (seekPosition > player.duration) {
				player.seekTo(player.duration)
			}
			player.seekTo(seekPosition)
		} catch (e: IllegalStateException) {
			e.printStackTrace()
		}
	}

	override fun rewindPlayerByNDuration(duration: Duration) {
		try {
			val seekPosition = player.currentPosition - duration.inWholeMilliseconds
			if (player.currentMediaItem == null) {
				Log.d(LOGGER, "PLAYER MEDIA ITEM NOT SET")
				return
			} else if (seekPosition < 0) {
				player.seekTo(0)
			}
			player.seekTo(seekPosition)
		} catch (e: IllegalStateException) {
			e.printStackTrace()
		}
	}

	override fun clearResources() {
		player.removeListener(_listener)
		Log.d(LOGGER, "REMOVED LISTENER FOR PLAYER")
		runBlocking {
			stopPlayer()
		}
	}

}