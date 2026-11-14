package com.eva.recorder.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
                val extras = Bundle().apply {
                    putInt(IntentConstants.EXTRAS_RECORDER_TIME, time?.toSecondOfDay() ?: 0)
                    putString(IntentConstants.EXTRAS_RECORDER_STATE, state.name)
                }
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
