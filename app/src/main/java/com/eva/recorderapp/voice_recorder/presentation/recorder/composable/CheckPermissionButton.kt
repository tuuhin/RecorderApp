package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R

@Composable
fun CheckPermissionButton(
	onPermissionChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = ButtonDefaults.shape,
	colors: ButtonColors = ButtonDefaults.buttonColors(),
	elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
	border: BorderStroke? = null,
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
	val context = LocalContext.current

	var hasRecordAudioPermission by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
					PermissionChecker.PERMISSION_GRANTED
		)
	}

	var hasNotificationPermission by remember {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			mutableStateOf(
				ContextCompat.checkSelfPermission(
					context,
					Manifest.permission.POST_NOTIFICATIONS
				) == PermissionChecker.PERMISSION_GRANTED
			)
		else mutableStateOf(true)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestMultiplePermissions()
	) { perms ->
		hasRecordAudioPermission = perms.getOrDefault(Manifest.permission.RECORD_AUDIO, false)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			hasNotificationPermission =
				perms.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false)
		}
		onPermissionChanged(hasRecordAudioPermission)
	}

	Button(
		modifier = modifier,
		shape = shape,
		colors = colors,
		elevation = elevation,
		border = border,
		contentPadding = contentPadding,
		onClick = {
			val perms = buildList {
				add(Manifest.permission.RECORD_AUDIO)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					add(Manifest.permission.POST_NOTIFICATIONS)
			}.toTypedArray()

			launcher.launch(perms)
		},
	) {
		Text(text = stringResource(id = R.string.allow_permissions))
	}
}