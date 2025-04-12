package com.eva.feature_recorder.composable

import android.Manifest
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
internal fun MicPermissionWrapper(
	modifier: Modifier = Modifier,
	onPermissionGranted: @Composable () -> Unit,
) {
	val context = LocalContext.current

	var hasRecordPermission by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
					PermissionChecker.PERMISSION_GRANTED
		)
	}

	Crossfade(
		targetState = hasRecordPermission,
		modifier = modifier,
		label = "Check if the application has microphone permission"
	) { hasPerms ->
		if (hasPerms) onPermissionGranted()
		else {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				NoRecordPermissionBox(
					onPermissionChanged = { perms -> hasRecordPermission = perms },
					modifier = Modifier.padding(12.dp)
				)
			}
		}
	}
}