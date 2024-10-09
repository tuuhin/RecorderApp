package com.eva.recorderapp.voice_recorder.widgets.recordings

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver


class RecordingsWidgetReceiver : GlanceAppWidgetReceiver() {

	override val glanceAppWidget: GlanceAppWidget
		get() = AppRecordingsWidget()
}