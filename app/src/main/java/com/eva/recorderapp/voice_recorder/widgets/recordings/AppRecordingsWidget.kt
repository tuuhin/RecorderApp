package com.eva.recorderapp.voice_recorder.widgets.recordings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.widgets.recordings.composables.RecordingsWidgetContent
import com.eva.recorderapp.voice_recorder.widgets.utils.RecorderAppWidgetTheme

private const val TAG = "APP_RECORDINGS_WIDGET"

class AppRecordingsWidget : GlanceAppWidget(R.layout.widget_loading_failed_layout) {

	override val sizeMode: SizeMode
		get() = SizeMode.Exact

	override suspend fun provideGlance(context: Context, id: GlanceId) {

		provideContent {

			RecorderAppWidgetTheme {
				RecordingsWidgetContent(
					resource = Resource.Success(data = emptyList()),
					onItemClick = { model ->
						try {
							val intent = model.prepareNavIntent(context)
							actionStartActivity(intent)

						} catch (e: Exception) {
							e.printStackTrace()
						}
					},
					onRefresh = { actionRunCallback<RefreshAction>() },
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
				val errorMessage = throwable.localizedMessage ?: "Some error occurred"
				setTextViewText(R.id.widget_error_description, errorMessage)
			}

		// show the error on the widget
		widgetManager.updateAppWidget(appWidgetId, remoteView)
	}

	override suspend fun onDelete(context: Context, glanceId: GlanceId) {
		super.onDelete(context, glanceId)
		Log.i(TAG, "WIDGET_REMOVED")
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


private fun RecordedVoiceModel.prepareNavIntent(context: Context): Intent =
	Intent().apply {
		setComponent(ComponentName(context, MainActivity::class.java))
		action = Intent.ACTION_VIEW
		data = NavDeepLinks.audioPlayerDestinationUri(id)
		addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	}