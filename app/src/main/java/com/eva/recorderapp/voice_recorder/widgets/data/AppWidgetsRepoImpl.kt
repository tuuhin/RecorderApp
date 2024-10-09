package com.eva.recorderapp.voice_recorder.widgets.data

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.util.AppWidgetsRepository
import com.eva.recorderapp.voice_recorder.widgets.recorder.AppRecorderWidget
import com.eva.recorderapp.voice_recorder.widgets.recorder.RecorderWidgetDefinition
import com.eva.recorderapp.voice_recorder.widgets.recordings.AppRecordingsWidget
import com.google.protobuf.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

private const val TAG = "UPDATE_APP_WIDGETS"

class AppWidgetsRepoImpl(private val context: Context) : AppWidgetsRepository {

	override suspend fun recordingsWidgetUpdate() {
		withContext(Dispatchers.Default) {
			Log.d(TAG, "RECORDINGS_WIDGET_UPDATED")
			AppRecordingsWidget().updateAll(context)
		}
	}

	override suspend fun recorderWidgetUpdate(state: RecorderState, time: LocalTime?) {
		val duration = Duration.newBuilder()
			.setSeconds(time?.second?.toLong() ?: 0L).build()

		val recordingMode = when (state) {
			RecorderState.RECORDING -> RecordingModeProto.RECORDING
			RecorderState.PAUSED -> RecordingModeProto.PAUSED
			else -> RecordingModeProto.IDLE_OR_COMPLETED
		}

		withContext(Dispatchers.IO) {
			// update the content
			RecorderWidgetDefinition.getDataStore(context, RecorderWidgetDefinition.FILE_LOCATION)
				.updateData { content ->
					content.toBuilder()
						.setMode(recordingMode)
						.setDuration(duration)
						.build()
				}
		}
		// update the widget
		withContext(Dispatchers.Default) {
			Log.d(TAG, "RECORDER_WIDGET_UPDATED ")
			AppRecorderWidget().updateAll(context)
		}
	}

	override suspend fun resetRecorderWidget() {
		val duration = RecorderWidgetDefinition.ZERO_DURATION
		val recordingMode = RecordingModeProto.IDLE_OR_COMPLETED

		withContext(Dispatchers.IO) {
			// update the content
			RecorderWidgetDefinition.getDataStore(context, RecorderWidgetDefinition.FILE_LOCATION)
				.updateData { content ->
					content.toBuilder()
						.setMode(recordingMode)
						.setDuration(duration)
						.build()
				}
		}
		// update the widget
		withContext(Dispatchers.Default) {
			Log.d(TAG, "RECORDER_WIDGET_RESET ")
			AppRecorderWidget().updateAll(context)
		}
	}
}