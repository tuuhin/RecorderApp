package com.eva.feature_widget.receivers

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.eva.feature_widget.recordings.AppRecordingsWidget
import com.eva.utils.IntentConstants
import kotlinx.coroutines.Dispatchers

class RecordingsWidgetReceiver : GlanceAppWidgetReceiver() {

	override val glanceAppWidget: GlanceAppWidget
		get() = AppRecordingsWidget()

	override fun onReceive(context: Context, intent: Intent) {
		super.onReceive(context, intent)
		// do stuff to update the widget
		if (intent.action != IntentConstants.ACTION_UPDATE_WIDGET) return
		// update the widget from here
		goAsync(Dispatchers.Main) {
			// do async stuff here
			AppRecordingsWidget().updateAll(context)
		}
	}
}