package com.eva.feature_onboarding.utils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

internal val Context.hasRecordAudioPermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		Manifest.permission.RECORD_AUDIO
	) == PermissionChecker.PERMISSION_GRANTED

internal val Context.hasNotificationPermission: Boolean
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.POST_NOTIFICATIONS
		) == PermissionChecker.PERMISSION_GRANTED
	} else true

internal val Context.hasReadAudioPermission: Boolean
	get() {
		val check = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
		else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
		return check == PermissionChecker.PERMISSION_GRANTED
	}

internal val Context.evaluateMissingPermission: Array<String>
	get() = buildList {
		if (!hasRecordAudioPermission) add(Manifest.permission.RECORD_AUDIO)
		if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			add(Manifest.permission.POST_NOTIFICATIONS)
		if (!hasReadAudioPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			add(Manifest.permission.READ_MEDIA_AUDIO)
		if (!hasReadAudioPermission) add(Manifest.permission.READ_EXTERNAL_STORAGE)
	}.toTypedArray()