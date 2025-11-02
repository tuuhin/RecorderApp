package com.eva.player.data.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.core.os.bundleOf
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.eva.player.data.service.MediaPlayerService
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.tryWithLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.time.Duration

private const val TAG = "PLAYED_MEDIA_CONTROLLER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class MediaControllerProvider(private val context: Context) : AudioFilePlayer {

	@Volatile
	private var _controller: MediaController? = null
	private var _lock = Mutex()

	private val _playerFlow = MutableStateFlow<AudioFilePlayer?>(null)
	private val _isConnected = MutableStateFlow(false)

	private val _currentPlayer: Flow<AudioFilePlayer> =
		combine(_isConnected, _playerFlow) { connected, player ->
			if (connected && player != null) player else null
		}.filterNotNull()

	private val player: AudioFilePlayer?
		get() = _playerFlow.value

	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _currentPlayer.flatMapLatest { player -> player.trackInfoAsFlow }

	override val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = _currentPlayer.flatMapLatest { player -> player.playerMetaDataFlow }

	override val isPlaying: Flow<Boolean>
		get() = _currentPlayer.flatMapLatest { player -> player.isPlaying }

	override val isControllerReady: Flow<Boolean>
		get() = _isConnected

	@androidx.annotation.OptIn(UnstableApi::class)
	private val _controllerListener = object : MediaController.Listener {

		override fun onDisconnected(controller: MediaController) {
			super.onDisconnected(controller)
			Log.i(TAG, "MEDIA CONTROLLER DISCONNECTED")
			// update is connected
			_isConnected.update { false }
			// clear the player
			val oldInstance = _playerFlow.getAndUpdate { null }
			oldInstance?.cleanUp()
		}

		override fun onError(controller: MediaController, sessionError: SessionError) {
			super.onError(controller, sessionError)
			Log.e(TAG, "MEDIA CONTROLLER ERROR :${sessionError.message}")
		}
	}

	override suspend fun prepareController(audioId: Long) {

		val sessionExtras = bundleOf(MediaPlayerService.PLAYER_AUDIO_FILE_ID_KEY to audioId)

		val sessionToken = SessionToken(
			context,
			ComponentName(context, MediaPlayerService::class.java)
		)
		_lock.tryWithLock(this) {
			if (_controller != null) {
				Log.d(TAG, "CONTROLLER IS ALREADY SET ")
				return
			}
			try {
				Log.d(TAG, "PREPARING THE PLAYER")
				// prepare the controller future
				withContext(Dispatchers.Main.immediate) {
					MediaController.Builder(context, sessionToken)
						.setConnectionHints(sessionExtras)
						.setListener(_controllerListener)
						.buildAsync()
						.await()
						.also { instance -> _controller = instance }
				}
				Log.i(TAG, "CONTROLLER CREATED")
				// set the player instance
				_isConnected.update { _controller?.isConnected ?: false }
				_playerFlow.value = _controller?.let { player -> AudioFilePlayerImpl(player) }
			} catch (e: Exception) {
				Log.e(TAG, "FAILED TO RESOLVE FUTURE", e)
				e.printStackTrace()
			}
		}
	}

	override suspend fun preparePlayer(audio: AudioFileModel): Result<Boolean> {
		if (_controller == null) {
			Log.d(TAG, "CONTROLLER IS NOT SET")
			return Result.failure(Exception("Controller is not set"))
		}
		Log.d(TAG, "PREPARING PLAYER")
		return player?.preparePlayer(audio) ?: Result.failure(Exception("Player is not set"))
	}

	override fun cleanUp() {
		Log.d(TAG, "CLEARING UP CONTROLLER")
		// release the controller if not released
		_controller?.release()
		_controller = null
		// perform player cleanup
		val oldInstance = _playerFlow.getAndUpdate { null }
		oldInstance?.cleanUp()
	}

	override fun onMuteDevice() {
		player?.onMuteDevice()
	}

	override fun onSeekDuration(duration: Duration) {
		player?.onSeekDuration(duration)
	}

	override fun seekPlayerByNDuration(duration: Duration, rewind: Boolean) {
		player?.seekPlayerByNDuration(duration, rewind)
	}

	override fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) {
		player?.setPlayBackSpeed(playBackSpeed)
	}

	override fun setPlayLooping(loop: Boolean) {
		player?.setPlayLooping(loop)
	}

	override suspend fun pausePlayer() {
		player?.pausePlayer()
	}

	override suspend fun startOrResumePlayer() {
		player?.startOrResumePlayer()
	}

	override suspend fun stopPlayer() {
		player?.stopPlayer()
	}
}