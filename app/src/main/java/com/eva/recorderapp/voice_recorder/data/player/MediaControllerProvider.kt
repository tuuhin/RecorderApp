package com.eva.recorderapp.voice_recorder.data.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.eva.recorderapp.common.PlayerConstants
import com.eva.recorderapp.voice_recorder.data.service.MediaPlayerService
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MEDIA_CONTROLLER_PROVIDER"

class MediaControllerProvider(
	private val context: Context
) {

	private var _future: ListenableFuture<MediaController>? = null

	private var _player = MutableStateFlow<AudioFilePlayer?>(null)
	private val _isConnected = MutableStateFlow(false)

	val playerFlow: Flow<AudioFilePlayer>
		get() = _player.filterNotNull()

	@OptIn(ExperimentalCoroutinesApi::class)
	val trackDataFlow: Flow<PlayerTrackData>
		get() = playerFlow.flatMapLatest { player -> player.trackInfoAsFlow }

	@OptIn(ExperimentalCoroutinesApi::class)
	val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = playerFlow.flatMapLatest { player -> player.playerMetaDataFlow }

	@OptIn(FlowPreview::class)
	val isControllerConnected: Flow<Boolean>
		get() = _isConnected.debounce(50.milliseconds)

	val player: AudioFilePlayer?
		get() = _player.value

	private val sessionToken: SessionToken
		get() = SessionToken(
			context,
			ComponentName(context, MediaPlayerService::class.java)
		)

	@androidx.annotation.OptIn(UnstableApi::class)
	private val controllerListener = object : MediaController.Listener {

		override fun onDisconnected(controller: MediaController) {
			super.onDisconnected(controller)
			Log.i(TAG, "MEDIA CONTROLLER DISCONNECTED")
			_isConnected.update { controller.isConnected }
			// remove listeners
			player?.cleanUp()
			_player.update { null }
		}

		override fun onError(controller: MediaController, sessionError: SessionError) {
			super.onError(controller, sessionError)
			Log.e(TAG, "MEDIA CONTROLLER ERROR :${sessionError.message}")
		}
	}

	private val controllerCallback = object : FutureCallback<MediaController> {
		override fun onSuccess(result: MediaController?) {

			result?.let { controller ->
				Log.i(TAG, "CONTROLLER CREATED")
				val appPlayer = controller.appPlayer
				_isConnected.update { controller.isConnected }
				_player.update { appPlayer }
			}
		}

		override fun onFailure(t: Throwable) {
			Log.e(TAG, "FAILED TO RESOLVE FUTURE", t)
		}
	}


	fun prepareController(audioId: Long) {
		try {
			val sessionExtras = bundleOf(
				PlayerConstants.PLAYER_AUDIO_FILE_ID_KEY to audioId
			)
			_future = MediaController.Builder(context, sessionToken)
				.setConnectionHints(sessionExtras)
				.setListener(controllerListener)
				.buildAsync()

			Futures.addCallback(_future!!, controllerCallback, MoreExecutors.directExecutor())
		} catch (e: Exception) {
			Log.e(TAG, "EXCEPTION WHILE CREATING CONTROLLER", e)
		}
	}

	fun removeController() {
		_future?.let(MediaController::releaseFuture)
		_future = null
		Log.d(TAG, "FUTURE FOR MEDIA CONTROLLER RELEASED")
	}

}

private val MediaController.appPlayer: AudioFilePlayer
	get() = AudioFilePlayerImpl(this)