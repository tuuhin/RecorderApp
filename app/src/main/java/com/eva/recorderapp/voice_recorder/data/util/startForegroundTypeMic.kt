package com.eva.recorderapp.voice_recorder.data.util

import android.app.ForegroundServiceStartNotAllowedException
import android.app.ForegroundServiceTypeException
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log


private const val LOGGER_TAG = "MIC_FORGROUND_SERVICE"

fun Service.startForegroundServiceMicrophone(id: Int, notification: Notification) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
		try {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
		} catch (e: ForegroundServiceTypeException) {
			Log.e(LOGGER_TAG, "WRONG FG-SERVICE TYPE", e)
		} catch (e: ForegroundServiceStartNotAllowedException) {
			Log.e(LOGGER_TAG, "FG-SERVICE NOT ALLOWED TO START", e)
		}
	} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		try {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
		} catch (e: ForegroundServiceStartNotAllowedException) {
			Log.e(LOGGER_TAG, "FG-SERVICE NOT ALLOWED TO START", e)
		}
	} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		try {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
		} catch (e: Exception) {
			Log.e(LOGGER_TAG, "SOME EXCEPTION OCCURED", e)
		}
	} else {
		try {
			startForeground(id, notification)
		} catch (e: Exception) {
			Log.e(LOGGER_TAG, "SOME EXCEPTION OCCURED", e)
		}
	}
}