package com.eva.recorder.data.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.ForegroundServiceTypeException
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log

private const val LOGGER_TAG = "MIC_FOREGROUND_SERVICE"

internal fun Service.startVoiceRecorderService(id: Int, notification: Notification) {
	try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
		else startForeground(id, notification)
	} catch (e: Exception) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && e is ForegroundServiceTypeException)
			Log.e(LOGGER_TAG, "WRONG FG TYPE", e)
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException)
			Log.e(LOGGER_TAG, "FG-SERVICE NOT ALLOWED TO START", e)
		else
			Log.e(LOGGER_TAG, "SOME EXCEPTION OCCURRED", e)
	}
}