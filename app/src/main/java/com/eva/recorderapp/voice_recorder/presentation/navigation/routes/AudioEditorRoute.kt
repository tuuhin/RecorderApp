package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.record_editor.RecordEditor


fun NavGraphBuilder.audioEditorRoute(
	controller: NavController
) = animatedComposable<NavRoutes.AudioEditor> {
	RecordEditor(
		navigation = {
			IconButton(
				onClick = dropUnlessResumed {
					if (controller.previousBackStackEntry?.destination?.route != null)
						controller.popBackStack()
				}
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Default.ArrowBack,
					contentDescription = stringResource(R.string.back_arrow)
				)
			}
		},
	)
}