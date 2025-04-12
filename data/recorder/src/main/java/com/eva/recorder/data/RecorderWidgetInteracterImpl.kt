package com.eva.recorder.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import com.eva.recorder.domain.RecorderWidgetInteractor
import com.eva.recorder.domain.models.RecorderState
import com.eva.utils.IntentConstants
import kotlinx.datetime.LocalTime

private const val TAG = "RECORDER_WIDGET_INTERACTION"

class RecorderWidgetInteracterImpl(private val context: Context) : RecorderWidgetInteractor {

	override suspend fun updateWidget(state: RecorderState, time: LocalTime?) {
		try {
			val intent = Intent().apply {
				setClassName(context.applicationContext, IntentConstants.RECORDER_WIDGET_RECEIVER)
				action = IntentConstants.ACTION_UPDATE_WIDGET
				val extras = bundleOf(
					IntentConstants.EXTRAS_RECORDER_TIME to time?.toSecondOfDay(),
					IntentConstants.EXTRAS_RECORDER_STATE to state.name
				)
				putExtras(extras)
			}
			context.sendBroadcast(intent)
		} catch (e: Exception) {
			Log.d(TAG, "SOME ERROR", e)
		}
	}

	override suspend fun resetWidget() {
		try {
			val intent = Intent().apply {
				setClassName(context.applicationContext, IntentConstants.RECORDER_WIDGET_RECEIVER)
				action = IntentConstants.ACTION_RESET_RECORDING_WIDGET
			}
			context.sendBroadcast(intent)
		} catch (e: Exception) {
			Log.d(TAG, "SOME ERROR", e)
		}
	}
}