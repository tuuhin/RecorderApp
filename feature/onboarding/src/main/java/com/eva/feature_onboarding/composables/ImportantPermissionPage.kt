package com.eva.feature_onboarding.composables

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_onboarding.utils.evaluateMissingPermission
import com.eva.feature_onboarding.utils.hasNotificationPermission
import com.eva.feature_onboarding.utils.hasReadAudioPermission
import com.eva.feature_onboarding.utils.hasRecordAudioPermission
import com.eva.ui.R
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun ImportantPermissionsPage(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	val context = LocalContext.current

	var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission) }
	var hasMicPermission by remember { mutableStateOf(context.hasRecordAudioPermission) }
	var hasMediaReadPermission by remember { mutableStateOf(context.hasReadAudioPermission) }

	val permissionsGranted by remember(
		hasMicPermission,
		hasMediaReadPermission,
		hasNotificationPermission
	) {
		derivedStateOf { hasMicPermission && hasNotificationPermission && hasMediaReadPermission }
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestMultiplePermissions(),
		onResult = { result ->
			if (result.containsKey(Manifest.permission.RECORD_AUDIO)) {
				hasMicPermission = result[Manifest.permission.RECORD_AUDIO] == true
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && result.containsKey(Manifest.permission.POST_NOTIFICATIONS)) {
				hasNotificationPermission = result[Manifest.permission.POST_NOTIFICATIONS] == true
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && result.containsKey(Manifest.permission.READ_MEDIA_AUDIO)) {
				hasMediaReadPermission = result[Manifest.permission.READ_MEDIA_AUDIO] == true
			}
			if (result.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE)) {
				hasMediaReadPermission = result[Manifest.permission.READ_EXTERNAL_STORAGE] == true
			}
		},
	)


	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(contentPadding),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Image(
			painter = painterResource(R.drawable.ic_security),
			contentDescription = "Permission security",
			colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.tertiary),
			modifier = Modifier.size(80.dp)
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(R.string.onboarding_page_title_permissions),
			style = MaterialTheme.typography.headlineMedium,
			fontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
			color = MaterialTheme.colorScheme.primary,
		)
		Spacer(modifier = Modifier.height(6.dp))
		Text(
			text = stringResource(R.string.onboarding_page_title_permissions_text),
			style = MaterialTheme.typography.titleSmall
		)
		ListItem(
			headlineContent = { Text(text = stringResource(R.string.onboarding_page_permission_mic)) },
			supportingContent = { Text(text = stringResource(R.string.onboarding_page_permission_mic_desc)) },
			leadingContent = {
				Icon(imageVector = Icons.Outlined.Mic, contentDescription = null)
			},
		)
		ListItem(
			headlineContent = { Text(text = stringResource(R.string.onboarding_page_permission_media)) },
			supportingContent = { Text(text = stringResource(R.string.onboarding_page_permission_media_desc)) },
			leadingContent = {
				Icon(imageVector = Icons.Outlined.MusicNote, contentDescription = null)
			},
		)
		ListItem(
			headlineContent = { Text(text = stringResource(R.string.onboarding_page_permission_notification)) },
			supportingContent = { Text(text = stringResource(R.string.onboarding_page_permission_notification_desc)) },
			leadingContent = {
				Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null)
			},
		)
		HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
		Text(
			text = stringResource(R.string.onboarding_page_title_permissions_extras),
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(6.dp))
		AnimatedContent(
			targetState = permissionsGranted
		) { isGranted ->
			if (isGranted) {
				Text(
					text = stringResource(R.string.onboarding_page_permission_all_granted),
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.secondary,
				)
			} else {
				Button(
					onClick = { launcher.launch(context.evaluateMissingPermission) },
					modifier = Modifier.widthIn(min = 240.dp),
					contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
					shape = MaterialTheme.shapes.medium,
				) {
					Text(text = stringResource(R.string.allow_permissions))
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun ImportantPermissionsPagePreview() = RecorderAppTheme {
	Surface {
		ImportantPermissionsPage(contentPadding = PaddingValues(12.dp))
	}
}