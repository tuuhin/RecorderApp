package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import android.Manifest
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@Composable
fun MediaAccessPermissionWrapper(
	onLoadRecordings: () -> Unit,
	modifier: Modifier = Modifier,
	onGranted: @Composable () -> Unit,
) {

	val context = LocalContext.current

	var hasStoragePermission by remember {
		val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
		else
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

		mutableStateOf(perms == PermissionChecker.PERMISSION_GRANTED)
	}

	LaunchedEffect(hasStoragePermission) {
		if (!hasStoragePermission) return@LaunchedEffect
		// populate recordings is permission is provided
		onLoadRecordings()
	}

	Crossfade(
		targetState = hasStoragePermission,
		label = "Permissions handling for external media access",
		modifier = modifier
	) { isGranted ->
		if (isGranted) onGranted()
		else Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			NoReadMusicPermissionBox(
				onPermissionChanged = { perms -> hasStoragePermission = perms },
				modifier = Modifier.padding(12.dp)
			)
		}
	}
}