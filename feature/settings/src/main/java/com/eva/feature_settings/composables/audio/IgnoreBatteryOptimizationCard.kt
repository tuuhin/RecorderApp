package com.eva.feature_settings.composables.audio

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun IgnoreBatteryOptimizationCard(
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
	contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
	val context = LocalContext.current
	Card(
		modifier = modifier,
		onClick = { context.launchIgnoreBatteryOptimizations() },
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.tertiaryContainer,
			contentColor = MaterialTheme.colorScheme.onTertiaryContainer
		),
		shape = MaterialTheme.shapes.extraLarge,
	) {
		Row(
			modifier = Modifier.padding(all = dimensionResource(id = R.dimen.card_padding)),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Box(
				modifier = Modifier
					.sizeIn(minWidth = 32.dp, minHeight = 32.dp)
					.background(
						color = contentColor,
						shape = MaterialTheme.shapes.extraLarge
					),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_battery_slanted),
					contentDescription = stringResource(id = R.string.turn_off_optimization),
					tint = containerColor
				)
			}
			Text(
				text = stringResource(id = R.string.turn_off_optimization),
				style = MaterialTheme.typography.labelMedium,
				modifier = Modifier
					.weight(1f)
					.padding(horizontal = 4.dp)
			)
		}
	}
}

@SuppressLint("BatteryLife")
private fun Context.launchIgnoreBatteryOptimizations() {
	try {
		val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
			.apply {
				data = Uri.fromParts("package", packageName, null)

				addCategory(Intent.CATEGORY_DEFAULT)
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

				addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
				addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
			}

		startActivity(intent)

	} catch (_: ActivityNotFoundException) {
		Toast.makeText(this, R.string.cannot_launch_activity, Toast.LENGTH_SHORT).show()
	}
}

@PreviewLightDark
@Composable
private fun IgnoreBatteryOptimizationCardPreview() = RecorderAppTheme {
	IgnoreBatteryOptimizationCard()
}