package com.eva.recordings.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.eva.recordings.domain.widgets.RecordingWidgetInteractor
import com.eva.utils.IntentConstants

private const val TAG = "RECORDINGS_WIDGET_INTERACTIONS"

internal class RecordingWidgetInteractorImpl(private val context: Context) :
	RecordingWidgetInteractor {

	override fun invoke() {
		try {
			val intent = Intent().apply {
				setClassName(context.applicationContext, IntentConstants.RECORDINGS_WIDGET_RECEIVER)
				action = IntentConstants.ACTION_UPDATE_WIDGET
			}
			context.sendBroadcast(intent)
		} catch (e: Exception) {
			Log.d(TAG, "SOME ERROR", e)
		}
	}
}