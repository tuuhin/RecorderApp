package com.eva.feature_widget.receivers

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.eva.feature_widget.recorder.AppRecorderWidget
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepo
import com.eva.recorder.domain.models.RecorderState
import com.eva.utils.IntentConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class RecorderWidgetReceiver : GlanceAppWidgetReceiver() {

	@Inject
	lateinit var recorderWidgetRepo: RecorderWidgetRepo

	override val glanceAppWidget: GlanceAppWidget
		get() = AppRecorderWidget()

	override fun onReceive(context: Context, intent: Intent) {
		super.onReceive(context, intent)
		// receive the actions and do stuff
		if (intent.action == IntentConstants.ACTION_UPDATE_WIDGET) {
			val timeInSeconds = intent.getIntExtra(IntentConstants.EXTRAS_RECORDER_TIME, -1)
			val stateAsString = intent.getStringExtra(IntentConstants.EXTRAS_RECORDER_STATE)

			val localTime = LocalTime.fromSecondOfDay(timeInSeconds)
			val recorderState = try {
				RecorderState.valueOf(stateAsString ?: "")
			} catch (e: Exception) {
				RecorderState.IDLE
			}
			goAsync {
				// update the data
				recorderWidgetRepo.updateRecordingWidget(recorderState, localTime)
				// then update the widget
				AppRecorderWidget().updateAll(context)
			}
		}
		if (intent.action == IntentConstants.ACTION_RESET_RECORDING_WIDGET) {
			goAsync {
				// reset the data
				recorderWidgetRepo.resetRecorderWidget()
				// update the widget
				AppRecorderWidget().updateAll(context)
			}
		}
	}
}