package com.eva.feature_onboarding.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.ui.R
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun WelcomePage(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(contentPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(R.drawable.ic_mic_variant),
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
			contentDescription = "probable logo",
			modifier = Modifier.size(64.dp),
		)
		Spacer(modifier = Modifier.height(72.dp))
		Text(
			text = buildString {
				append("Welcome to ")
				append(stringResource(R.string.app_name))
			},
			style = MaterialTheme.typography.headlineMedium,
			fontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
			color = MaterialTheme.colorScheme.primary,
		)
		Spacer(modifier = Modifier.height(6.dp))
		Text(
			text = stringResource(R.string.onboarding_page_welcome_page_text),
			style = MaterialTheme.typography.bodyLarge,
			textAlign = TextAlign.Center
		)
	}
}

@PreviewLightDark
@Composable
private fun WelcomePagePreview() = RecorderAppTheme {
	Surface {
		WelcomePage()
	}
}