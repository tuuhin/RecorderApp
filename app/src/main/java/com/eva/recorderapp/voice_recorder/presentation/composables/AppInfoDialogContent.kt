package com.eva.recorderapp.voice_recorder.presentation.composables

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.common.DeveloperInformation
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDialogInfoContent(
	modifier: Modifier = Modifier,
) {

	val inspectionMode = LocalInspectionMode.current
	val context = LocalContext.current

	val versioncode: String = remember {
		if (inspectionMode) return@remember "0.0.0"
		val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
		return@remember pInfo?.versionName ?: "0.0.0"
	}

	Box(
		modifier = modifier.sizeIn(minWidth = 280.dp, maxWidth = 280.dp),
		propagateMinConstraints = true
	) {
		Surface(
			shape = AlertDialogDefaults.shape,
			tonalElevation = AlertDialogDefaults.TonalElevation,
			color = AlertDialogDefaults.containerColor,
			contentColor = contentColorFor(AlertDialogDefaults.containerColor),
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_launcher_foreground),
						contentDescription = stringResource(R.string.app_name),
						modifier = Modifier.size(32.dp),
						tint = MaterialTheme.colorScheme.primary
					)
					Text(text = stringResource(id = R.string.app_name))
					Badge(
						containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						contentColor = MaterialTheme.colorScheme.onTertiaryContainer
					) {
						Text(text = versioncode, style = MaterialTheme.typography.labelMedium)
					}
				}
				FlowRow(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalArrangement = Arrangement.Center
				) {
					SuggestionChip(
						onClick = {
							context.viewAppProfile()
						},
						label = { Text(text = stringResource(id = R.string.app_info_source_code)) },
						shape = MaterialTheme.shapes.medium,
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
							context.viewGithubProfile()
						},
						label = { Text(text = stringResource(id = R.string.app_info_author)) },
						shape = MaterialTheme.shapes.medium,
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

private fun Context.viewGithubProfile() {
	try {
		Intent(Intent.ACTION_VIEW).apply {
			data = DeveloperInformation.GITHUB_PROFILE_LINK

			startActivity(this)
		}
	} catch (e: ActivityNotFoundException) {
		Toast.makeText(
			applicationContext, R.string.cannot_launch_activity, Toast.LENGTH_SHORT
		).show()
	}
}

private fun Context.viewAppProfile() {
	try {
		Intent(Intent.ACTION_VIEW).apply {
			data = DeveloperInformation.PROJECT_LINK

			startActivity(this)
		}
	} catch (e: ActivityNotFoundException) {
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