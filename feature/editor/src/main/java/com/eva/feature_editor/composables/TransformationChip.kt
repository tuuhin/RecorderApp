package com.eva.feature_editor.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eva.editor.domain.TransformationProgress

@Composable
fun TransformationChip(progress: TransformationProgress, modifier: Modifier = Modifier) {
	AnimatedVisibility(
		visible = progress != TransformationProgress.Idle,
		enter = slideInVertically() + expandIn(),
		exit = slideOutVertically() + shrinkOut(),
		modifier = modifier
	) {
		if (progress is TransformationProgress.Progress) {
			Text("Transforming :${progress.amount}")
		}

	}
}