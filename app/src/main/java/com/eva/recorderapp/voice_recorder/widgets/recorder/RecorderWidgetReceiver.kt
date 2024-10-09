package com.eva.recorderapp.voice_recorder.widgets.recorder

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class RecorderWidgetReceiver : GlanceAppWidgetReceiver() {

	override val glanceAppWidget: GlanceAppWidget
		get() = AppRecorderWidget()
}