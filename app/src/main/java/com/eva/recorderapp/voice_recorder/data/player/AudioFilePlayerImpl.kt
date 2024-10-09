package com.eva.recorderapp.voice_recorder.data.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.exceptions.CannotStartPlayerException
import com.eva.recorderapp.voice_recorder.domain.player.exceptions.SetPlayerCommandNotFound
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerTrackData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration

private const val LOGGER = "AUDIO_FILE_PLAYER"

class AudioFilePlayerImpl(
	private val player: Player,
) : AudioFilePlayer {

	private val _listener = AudioFilePlayerListener(player)

	override val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = _listener.playerMetaDataFlow

	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _listener.trackInfoAsFlow

	private val lock = Mutex()

	override fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) {
		val command = player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)
		if (!command) {
			Log.w(LOGGER, "CANNOT CHANGE SPEED COMMAND NOT AVAILABLE")
			return
		}
		player.setPlaybackSpeed(playBackSpeed.speed)
	}

	override fun setPlayLooping(loop: Boolean) {
		val command = player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)
		if (!command) {
			Log.w(LOGGER, "CANNOT REPEAT MODE COMMAND NOT AVAILABLE")
			return
		}
		val repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
		player.repeatMode = repeatMode
	}

	override fun onMuteDevice() {
		// TODO: Check proper implementation
		val command = player.isCommandAvailable(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
		if (!command) {
			Log.w(LOGGER, "PLAYER COMMAND NOT FOUND")
			return
		}
		val muted = !player.isDeviceMuted
		player.setDeviceMuted(muted, C.VOLUME_FLAG_VIBRATE)
	}

	override suspend fun preparePlayer(audio: AudioFileModel): Resource<Boolean, Exception> {
		val command = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)
		if (!command) {
			// command is not available
			return Resource.Error(SetPlayerCommandNotFound())
		}

		if (lock.holdsLock(this)) {
			Log.d(LOGGER, "METHOD IS LOCKED")
			// cannot start as the method is locked
			return Resource.Error(CannotStartPlayerException())
		}
		//  locking this w.r.t to this class
		lock.lock(this)
		try {
			player.addListener(_listener)
			Log.d(LOGGER, "PLAYER LISTENER ADDED")
			// current mediaId is same as the file audioId
			val areMediaIdSame = player.currentMediaItem?.mediaId == audio.id.toString()

			if (areMediaIdSame) {
				Log.d(LOGGER, "UPDATING PLAYER PARAMETERS")
				// player media item is set so need to update the parameters
				_listener.updateStateFromCurrentPlayerConfig()
			} else {
				Log.i(LOGGER, "MEDIA ITEM FOR AUDIO FILE : ${audio.id}")
				// normally adding the audio file to the player
				addAudioItemToPlayer(audio)
			}
			// prepare the player if the state is idle
			if (player.playbackState == Player.STATE_IDLE) {
				player.prepare()
				Log.d(LOGGER, "PLAYER PREPARED ")
			}
			// play audio when ready
			player.playWhenReady = true
			return Resource.Success(true)
		} catch (e: IllegalStateException) {
			Log.e(LOGGER, "PLAYER IS NOT CONFIGURED PROPERLY", e)
			return Resource.Error(e, message = "PLAYER IS NOT CONFIGURED PROPERLY")
		} catch (e: Exception) {
			e.printStackTrace()
			return Resource.Error(e)
		} finally {
			lock.unlock(this)
		}
	}


	override suspend fun pausePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(LOGGER, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
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
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(LOGGER, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
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
		val command = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
		if (!command) {
			Log.w(LOGGER, "PLAYER SEEK IN MEDIA COMMAND NOT FOUND")
			return
		}
		val totalDuration = player.duration
		val changedDuration = duration.inWholeMilliseconds
		if (changedDuration <= totalDuration) {
			Log.d(LOGGER, "SEEK POSITION $duration")
			player.seekTo(duration.inWholeMilliseconds)
		}
	}

	override fun seekPlayerByNDuration(duration: Duration, rewind: Boolean) {
		val command = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
		if (!command) {
			Log.w(LOGGER, "PLAYER SEEK IN MEDIA COMMAND NOT FOUND")
			return
		}
		val amount = duration.inWholeMilliseconds.run {
			if (rewind) unaryMinus() else this
		}
		val seekPosition = player.currentPosition + amount

		if (seekPosition >= player.duration) {
			// seek to max duration
			Log.d(LOGGER, "SEEK POSITION IS PLAYER DURATION")
			player.seekTo(player.duration)
		} else if (seekPosition < 0) {
			Log.d(LOGGER, "SEEK POSITION IS LESSER THAN 0")
			player.seekTo(0)
		} else {
			Log.d(LOGGER, "PLAYER POSITION CHANGED $seekPosition")
			player.seekTo(seekPosition)
		}
	}

	override fun cleanUp() {
		player.removeListener(_listener)
		Log.d(LOGGER, "REMOVED LISTENER FOR PLAYER")
	}

	private fun addAudioItemToPlayer(audio: AudioFileModel) {
		val mediaItem = audio.toMediaItem()
		// set this current media item
		player.apply {
			// set repeat mode off
			repeatMode = Player.REPEAT_MODE_OFF
			// set speed to 1f
			setPlaybackSpeed(1f)
			// clear and set item
			clearMediaItems()
			setMediaItem(mediaItem)
		}
		Log.d(LOGGER, "MEDIA ITEM ADDED MEDIA COUNT:${player.mediaItemCount}")
		if (player.playbackState != Player.STATE_IDLE) {
			Log.d(LOGGER, "STOPPING PLAYER ")
			// if the player is not in idle state stop the player
			player.stop()
		}
	}
}