package com.eva.recorderapp.voice_recorder.presentation.record_player

import android.content.ComponentName
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.eva.recorderapp.common.PlayerConstants
import com.eva.recorderapp.voice_recorder.data.service.MediaPlayerService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException

private const val TAG = "PLAYER_CONTROLLER_PROVIDER"

@Composable
fun PlayerMediaControllerProvider(
	audioId: Long,
	onPlayerReady: @Composable (Player) -> Unit,
	onOther: @Composable () -> Unit,
	modifier: Modifier = Modifier,
) {

	val lifeCycleOwner = LocalLifecycleOwner.current
	val context = LocalContext.current

	var player: Player? by remember { mutableStateOf(null) }


	DisposableEffect(key1 = lifeCycleOwner) {

		val sessionToken = SessionToken(
			context,
			ComponentName(context, MediaPlayerService::class.java)
		)

		val sessionExtras = bundleOf(
			PlayerConstants.PLAYER_AUDIO_FILE_ID_KEY to audioId
		)

		val controller = MediaController.Builder(context, sessionToken)
			.setConnectionHints(sessionExtras)
			.buildAsync()

		val observer = LifecycleEventObserver { _, event ->

			if (event == Lifecycle.Event.ON_START) {
				val runnable = Runnable {
					try {
						if (controller.isDone) {
							Log.i(TAG, "MEDIA CONTROLLER SET")
							player = controller.get()
						}
					} catch (e: ExecutionException) {
						Log.e(TAG, "EXECUTION EXCEPTION WHILE GETTING THE CONTROLLER", e)
					}
				}
				controller.addListener(runnable, MoreExecutors.directExecutor())
				Log.d(TAG, "LISTENER FOR FUTURE MEDIA CONTROLLER ADDED")

			}

			if (event == Lifecycle.Event.ON_STOP) {
				MediaController.releaseFuture(controller)
				player = null
				Log.d(TAG, "FUTURE FOR MEDIA CONTROLLER RELEASED")
			}
		}

		// add the lifecyle observer
		Log.d(TAG, "ADDED LIFECYCLE OBSERVER")
		lifeCycleOwner.lifecycle.addObserver(observer)

		onDispose {
			// cancel the ongoing future if present
			Log.i(TAG, "CANCELLING MEDIA CONTROLLER")
			controller.cancel(true)
			// remove the lifecyle observer
			Log.d(TAG, "REMOVED LIFECYCLE OBSERVER")
			lifeCycleOwner.lifecycle.removeObserver(observer)
		}
	}

	Crossfade(
		targetState = player != null,
		label = "Setting the recorder animation",
		modifier = modifier,
	) { isReady ->
		if (player != null && isReady) onPlayerReady(player!!)
		else onOther()
	}
}