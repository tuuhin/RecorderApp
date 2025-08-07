package com.eva.feature_onboarding.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
internal fun OnBoardingBottomBar(
	progress: () -> Float,
	showPreviousButton: Boolean,
	showNextOrContinueActionButton: Boolean,
	showContinueAction: Boolean,
	modifier: Modifier = Modifier,
	onPrevious: () -> Unit = {},
	onNext: () -> Unit = {},
	onContinueToContent: () -> Unit = {},
	windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
	contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
) {
	Column(
		verticalArrangement = Arrangement.Center,
		modifier = modifier
			.fillMaxWidth()
			.windowInsetsPadding(windowInsets)
			.height(100.dp)
			.padding(contentPadding)
	) {
		LinearProgressIndicator(
			progress = progress,
			strokeCap = StrokeCap.Round,
			modifier = Modifier.fillMaxWidth(),
		)
		Spacer(modifier = Modifier.height(4.dp))
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth(),
		) {
			AnimatedVisibility(
				visible = showPreviousButton,
				enter = slideInHorizontally(),
				exit = slideOutHorizontally()
			) {
				Button(
					onClick = onPrevious,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					),
					shape = MaterialTheme.shapes.medium,
				) {
					Text(text = "Previous")
				}
			}
			Spacer(modifier = Modifier.weight(1f))
			AnimatedVisibility(
				visible = showNextOrContinueActionButton,
				enter = slideInHorizontally(),
				exit = slideOutHorizontally()
			) {
				if (showContinueAction) {
					Button(
						onClick = onContinueToContent,
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer,
							contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
						),
						shape = MaterialTheme.shapes.medium,
					) {
						Text("Continue")
					}
				} else {
					Button(
						onClick = onNext,
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.tertiaryContainer,
							contentColor = MaterialTheme.colorScheme.onTertiaryContainer
						),
						shape = MaterialTheme.shapes.medium,
					) { Text("Next") }
				}
			}
		}
	}
}