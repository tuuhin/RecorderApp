package com.eva.recorder.data

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

internal val Context.hasAudioRecordPermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		Manifest.permission.RECORD_AUDIO
	) == PermissionChecker.PERMISSION_GRANTED