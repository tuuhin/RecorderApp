package com.eva.recorderapp.navigation.routes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.BuildConfig
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.ApplicationInfo

fun NavGraphBuilder.appInfoDialog() = dialog<NavDialogs.ApplicationInfo> {
	AppDialogInfoContent()
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppDialogInfoContent(
	modifier: Modifier = Modifier,
	shape: Shape = AlertDialogDefaults.shape,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	titleColor: Color = AlertDialogDefaults.titleContentColor,
	iconColor: Color = AlertDialogDefaults.iconContentColor,
	color: Color = AlertDialogDefaults.containerColor,
	contentColor: Color = contentColorFor(color),
) {
	val context = LocalContext.current

	Box(
		modifier = modifier.sizeIn(minWidth = 280.dp, maxWidth = 280.dp),
		propagateMinConstraints = true
	) {
		Surface(
			shape = shape,
			tonalElevation = tonalElevation,
			color = color,
			contentColor = contentColor
		) {
			Column(
				modifier = Modifier.padding(20.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_launcher_foreground),
					contentDescription = stringResource(R.string.app_name),
					modifier = Modifier.size(56.dp),
					tint = iconColor
				)
				Text(
					text = stringResource(id = R.string.app_name),
					style = MaterialTheme.typography.titleLarge,
					color = titleColor
				)
				Row(
					horizontalArrangement = Arrangement.spacedBy(6.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = stringResource(id = R.string.app_version_title),
						style = MaterialTheme.typography.labelLarge
					)
					Badge(
						containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						contentColor = MaterialTheme.colorScheme.onTertiaryContainer
					) {
						Text(
							text = BuildConfig.VERSION_NAME,
							style = MaterialTheme.typography.labelMedium,
							modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
						)
					}
				}
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalArrangement = Arrangement.Center,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					SuggestionChip(
						onClick = {
							context.launchViewIntent(ApplicationInfo.GITHUB_PROJECT_LINK.toUri())
						},
						label = { Text(text = stringResource(id = R.string.app_info_source_code)) },
						shape = MaterialTheme.shapes.large,
						icon = {
							Icon(
								painter = painterResource(id = R.drawable.ic_code),
								contentDescription = stringResource(id = R.string.app_info_source_code)
							)
						},
						colors = SuggestionChipDefaults.suggestionChipColors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
							iconContentColor = MaterialTheme.colorScheme.primary
						)
					)
					SuggestionChip(
						onClick = {
							context.launchViewIntent(ApplicationInfo.PROJECT_AUTHOR_LINK.toUri())
						},
						label = { Text(text = stringResource(id = R.string.app_info_author)) },
						shape = MaterialTheme.shapes.large,
						icon = {
							Icon(
								painter = painterResource(id = R.drawable.ic_author),
								contentDescription = stringResource(id = R.string.app_info_author)
							)
						},
						colors = SuggestionChipDefaults.suggestionChipColors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
							iconContentColor = MaterialTheme.colorScheme.primary
						)
					)
				}
			}
		}
	}
}


private fun Context.launchViewIntent(uri: Uri) {
	try {
		Intent(Intent.ACTION_VIEW).apply {
			data = uri
			startActivity(this)
		}
	} catch (_: ActivityNotFoundException) {
		Toast.makeText(
			applicationContext,
			R.string.cannot_launch_activity,
			Toast.LENGTH_SHORT
		).show()
	}
}

@PreviewLightDark
@Composable
private fun AppInfoDialogContentPreview() = RecorderAppTheme {
	AppDialogInfoContent()
}