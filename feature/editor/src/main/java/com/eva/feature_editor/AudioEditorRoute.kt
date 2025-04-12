package com.eva.feature_editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
	animatedComposable<NavRoutes.AudioEditor> {
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