package com.eva.recorderapp.voice_recorder.widgets.recorder

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.widgets.data.RecorderWidgetDataProto
import com.eva.recorderapp.voice_recorder.widgets.data.toModel
import com.eva.recorderapp.voice_recorder.widgets.utils.RecorderAppWidgetTheme


class AppRecorderWidget : GlanceAppWidget() {

	override val sizeMode: SizeMode
		get() = SizeMode.Exact

	override val stateDefinition: GlanceStateDefinition<RecorderWidgetDataProto>
		get() = RecorderWidgetDefinition


	override suspend fun provideGlance(context: Context, id: GlanceId) {
		provideContent {
			val state = currentState<RecorderWidgetDataProto>()

			RecorderAppWidgetTheme {
				RecorderWidgetContent(
					model = state.toModel(),
					modifier = GlanceModifier
						.clickable(onClick = actionStartActivity<MainActivity>())
				)
			}
		}
	}

	override fun onCompositionError(
		context: Context,
		glanceId: GlanceId,
		appWidgetId: Int,
		throwable: Throwable,
	) {
		// print stacktrace
		throwable.printStackTrace()
		// update the layout
		val widgetManager = context.getSystemService<AppWidgetManager>() ?: return

		val remoteView = RemoteViews(context.packageName, R.layout.widget_loading_failed_layout)
			.apply {
				val message = throwable.message ?: context.getString(R.string.widget_error_text)
				setTextViewText(R.id.widget_error_description, message)
			}

		// show the error on the widget
		widgetManager.updateAppWidget(appWidgetId, remoteView)
	}

}