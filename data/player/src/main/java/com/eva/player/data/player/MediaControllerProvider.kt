package com.eva.player.data.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.concurrent.futures.await
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.eva.player.data.MediaPlayerConstants
import com.eva.player.data.service.MediaPlayerService
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.tryWithLock
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private const val TAG = "PLAYED_MEDIA_CONTROLLER"

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
internal class MediaControllerProvider(
    private val context: Context,
    private val mainImmediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : AudioFilePlayer {

    @Volatile
    private var _controller: MediaController? = null
    private var _lock = Mutex()

    private val _playerState: MutableStateFlow<MediaControllerState> =
        MutableStateFlow(MediaControllerState.Disconnected)

    private val player: AudioFilePlayer?
        get() = (_playerState.value as? MediaControllerState.Connected)?.player

    override val trackInfoAsFlow: Flow<PlayerTrackData>
        get() = _playerState.flatMapLatest { state ->
            when (state) {
                is MediaControllerState.Connected -> state.player.trackInfoAsFlow
                else -> emptyFlow()
            }
        }

    override val playerMetaDataFlow: Flow<PlayerMetaData>
        get() = _playerState.flatMapLatest { state ->
            when (state) {
                is MediaControllerState.Connected -> state.player.playerMetaDataFlow
                else -> emptyFlow()
            }
        }

    override val isPlaying: Flow<Boolean>
        get() = _playerState.flatMapLatest { state ->
            when (state) {
                is MediaControllerState.Connected -> state.player.isPlaying
                else -> flowOf(false)
            }
        }

    override val isControllerReady: Flow<Boolean>
        get() = _playerState.map { state -> state is MediaControllerState.Connected }

    private val _controllerListener = object : MediaController.Listener {

        override fun onDisconnected(controller: MediaController) {
            super.onDisconnected(controller)
            Log.i(TAG, "MEDIA CONTROLLER DISCONNECTED")
            // clear the player if its connected state
            val oldInstance = _playerState.value
            if (oldInstance is MediaControllerState.Connected)
                oldInstance.player.cleanUp()
            // then disconnect the controller
            _playerState.value = MediaControllerState.Disconnected
        }

        override fun onError(controller: MediaController, sessionError: SessionError) {
            super.onError(controller, sessionError)
            Log.e(TAG, "MEDIA CONTROLLER ERROR :${sessionError.message}")
        }
    }

    override suspend fun prepareController(audioId: Long) {
        if (_controller != null) {
            Log.d(TAG, "CONTROLLER IS ALREADY SET ")
            return
        }
        val sessionExtras = Bundle().apply {
            putLong(MediaPlayerConstants.PLAYER_AUDIO_FILE_ID_KEY, audioId)
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaPlayerService::class.java),
        )
        _lock.tryWithLock(this) {
            try {
                _playerState.value = MediaControllerState.Connecting
                Log.d(TAG, "PREPARING THE CONTROLLER")
                // prepare the controller future
                withContext(mainImmediateDispatcher) {
                    MediaController.Builder(context, sessionToken)
                        .setConnectionHints(sessionExtras)
                        .setListener(_controllerListener)
                        .buildAsync()
                        .await()
                        .also { instance -> _controller = instance }
                }
                Log.i(TAG, "CONTROLLER CREATED")
                // set the player instance
                val player = AudioFilePlayerImpl(_controller!!)
                _playerState.value = MediaControllerState.Connected(player)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "FAILED TO RESOLVE FUTURE", e)
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
        try {
            if (_controller == null) {
                Log.d(TAG, "CONTROLLER ALREADY RELEASED")
                return
            }
            // perform player cleanup
            val oldInstance = _playerState.value
            if (oldInstance is MediaControllerState.Connected)
                oldInstance.player.cleanUp()
            // release the controller if not released
            _controller?.release()

        } finally {
            Log.d(TAG, "CONTROLLER CLEANED")
            _controller = null
            _playerState.value = MediaControllerState.Disconnected
        }
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
