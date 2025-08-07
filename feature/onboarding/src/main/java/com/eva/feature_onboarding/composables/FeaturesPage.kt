package com.eva.feature_onboarding.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.eva.ui.R
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun FeaturesPage(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	val bulletColor = MaterialTheme.colorScheme.primary
	val bullets = stringArrayResource(R.array.onboarding_page_features)

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Spacer(modifier = Modifier.weight(1f))
		StackedImages(modifier = Modifier.align(Alignment.CenterHorizontally))
		Spacer(modifier = Modifier.weight(1f))
		Text(
			text = stringResource(R.string.onboarding_page_feature_title),
			style = MaterialTheme.typography.headlineMedium,
			color = MaterialTheme.colorScheme.primary,
			fontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
		)
		Spacer(modifier = Modifier.height(12.dp))
		bullets.forEach { text ->
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Canvas(modifier = Modifier.size(8.dp)) {
					drawCircle(color = bulletColor)
				}
				Text(
					text = text,
					style = MaterialTheme.typography.titleMedium
				)
			}
		}
	}
}

@Composable
private fun StackedImages(
	modifier: Modifier = Modifier,
	size: DpSize = DpSize(80.dp, 80.dp),
	color: Color = MaterialTheme.colorScheme.secondary
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			painter = painterResource(R.drawable.ic_recorder),
			contentDescription = "Recorder",
			colorFilter = ColorFilter.tint(color),
			modifier = Modifier
				.size(size)
				.offset(x = (size.width * -.7f), y = (size.height * -.5f))
		)
		Image(
			painter = painterResource(R.drawable.ic_music_player),
			contentDescription = "Player",
			colorFilter = ColorFilter.tint(color),
			modifier = Modifier
				.size(size)
				.offset(x = (size.width * -.8f), y = (size.height * .75f))
		)
		Image(
			painter = painterResource(R.drawable.ic_music_edit),
			contentDescription = "Edit",
			colorFilter = ColorFilter.tint(color),
			modifier = Modifier
				.size(size)
				.offset(x = (size.width * .5f), y = (size.height * .85f))
		)
		Image(
			painter = painterResource(R.drawable.ic_list),
			contentDescription = "Organize",
			colorFilter = ColorFilter.tint(color),
			modifier = Modifier
				.size(size)
				.offset(x = (size.width * .45f), y = (size.height * -.2f))
		)
	}
}

@PreviewLightDark
@Composable
private fun FeaturesPagePreview() = RecorderAppTheme {
	Surface {
		FeaturesPage(contentPadding = PaddingValues(12.dp))
	}
}