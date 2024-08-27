package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@Composable
fun requestReadStoragePermission(
	onPermissionChange: (Boolean) -> Unit = {}
): Boolean {

	val context = LocalContext.current

	var hasPermission by remember {
		val check = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
		else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
		mutableStateOf(check == PermissionChecker.PERMISSION_GRANTED)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		hasPermission = isGranted
		onPermissionChange(hasPermission)
	}

	LaunchedEffect(key1 = Unit) {
		if (hasPermission) return@LaunchedEffect

		val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			Manifest.permission.READ_MEDIA_AUDIO
		} else Manifest.permission.READ_EXTERNAL_STORAGE


		launcher.launch(permissions)
	}

	return hasPermission
}