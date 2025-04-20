package com.eva.feature_editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun PlayerTrimSelector(modifier: Modifier = Modifier) {
	Box(
		modifier = modifier
			.aspectRatio(1.5f)
			.background(MaterialTheme.colorScheme.surfaceContainer)
			.clip(MaterialTheme.shapes.extraLarge)
	) {

	}
}