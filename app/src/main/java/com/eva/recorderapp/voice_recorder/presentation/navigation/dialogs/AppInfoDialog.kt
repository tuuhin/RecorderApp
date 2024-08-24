package com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.util.composable.AppDialogInfoContent


fun NavGraphBuilder.appInfoDialog() = dialog<NavRoutes.ApplicationInfo>(
	dialogProperties = DialogProperties(),
) {
	Box(
		modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 280.dp),
		propagateMinConstraints = true
	) {
		AppDialogInfoContent()
	}
}