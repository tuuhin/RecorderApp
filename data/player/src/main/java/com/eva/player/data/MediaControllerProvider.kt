package com.eva.player.data

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
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "MEDIA_CONTROLLER_PROVIDER"

class MediaControllerProvider(private val context: Context) {

	private var _controller: MediaController? = null
	private var _player: AudioFilePlayer? = null

	private val _isConnected = MutableStateFlow(false)
	val isControllerConnected: StateFlow<Boolean>
		get() = _isConnected

	@OptIn(ExperimentalCoroutinesApi::class)
	val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _isConnected.flatMapLatest { _player?.trackInfoAsFlow ?: emptyFlow() }

	@OptIn(ExperimentalCoroutinesApi::class)
	val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = _isConnected.flatMapLatest { _player?.playerMetaDataFlow ?: emptyFlow() }

	val player: AudioFilePlayer?
		get() = _player

	@androidx.annotation.OptIn(UnstableApi::class)
	private val controllerListener = object : MediaController.Listener {

		override fun onDisconnected(controller: MediaController) {
			super.onDisconnected(controller)
			Log.i(TAG, "MEDIA CONTROLLER DISCONNECTED")
			// clear the player
			_player?.cleanUp()
			_player = null
			// update is connected
			_isConnected.update { controller.isConnected }
		}

		override fun onError(controller: MediaController, sessionError: SessionError) {
			super.onError(controller, sessionError)
			Log.e(TAG, "MEDIA CONTROLLER ERROR :${sessionError.message}")
		}
	}


	suspend fun prepareController(audioId: Long) {

		val sessionExtras = bundleOf(
			MediaPlayerService.PLAYER_AUDIO_FILE_ID_KEY to audioId
		)

		val sessionToken = SessionToken(
			context,
			ComponentName(context, MediaPlayerService::class.java)
		)
		try {
			Log.d(TAG, "PREPARING THE PLAYER")
			// prepare the controller future
			_controller = withContext(Dispatchers.Main.immediate) {
				MediaController.Builder(context, sessionToken)
					.setConnectionHints(sessionExtras)
					.setListener(controllerListener)
					.buildAsync()
					.await()
			}
			val controller = _controller ?: return
			Log.i(TAG, "CONTROLLER CREATED")
			_player = controller.appPlayer
			_isConnected.update { controller.isConnected }

		} catch (e: Exception) {
			Log.e(TAG, "FAILED TO RESOLVE FUTURE", e)
			e.printStackTrace()
		}
	}

	fun releaseController() {
		Log.d(TAG, "CLEARING UP CONTROLLER")
		// release the controller if not released
		_controller?.release()
		_controller = null
		// perform player cleanup
		_player?.cleanUp()
		_player = null
	}

	suspend fun preparePlayer(audio: AudioFileModel): Resource<Boolean, Exception>? {
		if (_controller == null) {
			Log.d(TAG, "CONTROLLER IS NOT SET")
			return null
		}
		Log.d(TAG, "PREPARING PLAYER")
		val results = player?.preparePlayer(audio)
		return results
	}

}

private val MediaController.appPlayer: AudioFilePlayer
	get() = AudioFilePlayerImpl(this)