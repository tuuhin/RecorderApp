package com.eva.feature_widget.recordings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import com.eva.feature_widget.R
import com.eva.feature_widget.di.WidgetsEntryPoint
import com.eva.feature_widget.recordings.composables.RecordingsWidgetContent
import com.eva.feature_widget.utils.RecorderAppWidgetTheme
import com.eva.utils.IntentConstants
import com.eva.utils.NavDeepLinks
import com.eva.utils.Resource
import dagger.hilt.EntryPoints


private const val TAG = "APP_RECORDINGS_WIDGET"

internal class AppRecordingsWidget : GlanceAppWidget(R.layout.widget_loading_failed_layout) {

	override val sizeMode: SizeMode
		get() = SizeMode.Exact

	override suspend fun provideGlance(context: Context, id: GlanceId) {

		val entryPoint = EntryPoints.get(
			context.applicationContext,
			WidgetsEntryPoint::class.java
		)

		val recordingsProvider = entryPoint.providesCurrentAppRecordings()

		provideContent {

			val resource by recordingsProvider.invoke().collectAsState(initial = Resource.Loading)

			RecorderAppWidgetTheme {
				RecordingsWidgetContent(
					resource = resource,
					onRefresh = { actionRunCallback<RefreshAction>() },
					modifier = GlanceModifier.clickable(
						onClick = actionStartActivity(
							Intent().apply {
								setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
								action = Intent.ACTION_VIEW
								data = NavDeepLinks.RECORDING_DESTINATION_PATTERN.toUri()
								flags = Intent.FLAG_ACTIVITY_NEW_TASK
							}
						)
					)
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


private class RefreshAction : ActionCallback {
	override suspend fun onAction(
		context: Context,
		glanceId: GlanceId,
		parameters: ActionParameters,
	) {
		Log.d(TAG, "REFRESH CALLED")
		AppRecordingsWidget().update(context, glanceId)
	}
}