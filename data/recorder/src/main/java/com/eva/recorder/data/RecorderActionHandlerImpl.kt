package com.eva.recorder.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.eva.recorder.data.service.VoiceRecorderService
import com.eva.recorder.domain.RecorderActionHandler
import com.eva.recorder.domain.models.RecorderAction
import com.eva.utils.Resource

private const val TAG = "RECORDER_ACTION_HANDLER"

internal class RecorderActionHandlerImpl(
	private val context: Context,
) : RecorderActionHandler {

	private val serviceIntent: Intent
		get() = Intent(context, VoiceRecorderService::class.java)

	private fun startRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.StartRecorderAction.action
		}
		context.startService(intent)
	}

	private fun resumeRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.ResumeRecorderAction.action
		}
		context.startService(intent)
	}

	private fun pauseRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.PauseRecorderAction.action
		}
		context.startService(intent)
	}

	private fun stopRecorder() {
		val intent = serviceIntent.apply {
			action = RecorderAction.StopRecorderAction.action
		}
		context.startService(intent)
	}

	private fun cancelRecording() {
		val intent = serviceIntent.apply {
			action = RecorderAction.CancelRecorderAction.action
		}
		context.startService(intent)
	}

	private fun addBookMarkAction() {
		val intent = serviceIntent.apply {
			action = RecorderAction.AddBookMarkAction.action
		}
		context.startService(intent)
	}

	override fun onRecorderAction(action: RecorderAction): Resource<Unit, Exception> {
		return try {
			when (action) {
				RecorderAction.StartRecorderAction -> startRecorder()
				RecorderAction.ResumeRecorderAction -> resumeRecorder()
				RecorderAction.PauseRecorderAction -> pauseRecorder()
				RecorderAction.StopRecorderAction -> stopRecorder()
				RecorderAction.CancelRecorderAction -> cancelRecording()
				RecorderAction.AddBookMarkAction -> addBookMarkAction()
			}
			Resource.Success(data = Unit)
		} catch (e: SecurityException) {
			Log.d(TAG, "SECURITY EXCEPTION", e)
			Resource.Error<Unit, Exception>(e)
		} catch (e: IllegalStateException) {
			Log.d(TAG, "SERVICE CANNOT BE STARTED", e)
			Resource.Error<Unit, Exception>(e)
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

}