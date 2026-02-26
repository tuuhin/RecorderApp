package com.eva.feature_recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.feature_recorder.util.TranslationDataBlock

@Composable
internal fun RecorderLiveTranscriptionText(
	result: TranslationDataBlock,
	modifier: Modifier = Modifier,
	labelTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
	fontFamily: FontFamily = FontFamily.Default,
	containerColor: Color = MaterialTheme.colorScheme.inverseSurface,
	contentColor: Color = contentColorFor(containerColor)
) {
	AnimatedContent(
		targetState = result,
		transitionSpec = {
			if (targetState.blockNumber != initialState.blockNumber) {
				scaleIn(
					initialScale = .4f,
					animationSpec = tween(durationMillis = 100, easing = EaseInBounce)
				) + fadeIn() togetherWith scaleOut(
					targetScale = .1f,
					animationSpec = tween(durationMillis = 80, easing = EaseOut)
				) + fadeOut()
			} else EnterTransition.None togetherWith ExitTransition.None
		},
		contentAlignment = Alignment.Center,
		modifier = modifier
	) { block ->
		if (!block.translationResult.isNullOrBlank()) {
			Surface(
				color = containerColor,
				shape = MaterialTheme.shapes.medium,
				modifier = Modifier.wrapContentSize()
			) {
				Text(
					text = block.translationResult,
					color = contentColor,
					fontFamily = fontFamily,
					modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
					style = labelTextStyle,
					maxLines = 2,
					textAlign = TextAlign.Center,
				)
			}
		} else {
			Spacer(modifier = Modifier.height(12.dp))
		}
	}
}