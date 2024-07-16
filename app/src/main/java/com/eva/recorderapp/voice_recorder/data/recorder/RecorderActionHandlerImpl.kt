package com.eva.recorderapp.voice_recorder.data.recorder

import android.content.Context
import android.content.Intent
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderActionHandler
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.services.VoiceRecorderService

class RecorderActionHandlerImpl(
	private val context: Context
) : RecorderActionHandler {

	private val serviceIntent: Intent
		get() = Intent(context, VoiceRecorderService::class.java)

	private fun startRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.START_RECORDER.action
		}
		context.startService(intent)
	}

	private fun resumeRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.RESUME_RECORDER.action
		}
		context.startService(intent)
	}

	private fun pauseRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.PAUSE_RECORDER.action
		}
		context.startService(intent)
	}

	private fun stopRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.STOP_RECORDER.action
		}
		context.startService(intent)
	}

	override fun onRecorderAction(action: RecorderAction) {
		when (action) {
			RecorderAction.START_RECORDER -> startRecorder()
			RecorderAction.RESUME_RECORDER -> resumeRecorder()
			RecorderAction.PAUSE_RECORDER -> pauseRecorder()
			RecorderAction.STOP_RECORDER -> stopRecorder()
		}
	}
}