package com.eva.feature_settings.composables.audio

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@SuppressLint("BatteryLife")
@Composable
internal fun IgnoreBatteryOptimizationCard(
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	Card(
		modifier = modifier,
		onClick = {
			try {
				// TODO: Read the official docs why its required
				val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
					data = Uri.fromParts("package", context.packageName, null)

					addCategory(Intent.CATEGORY_DEFAULT)
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

					addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
					addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
				}

				context.startActivity(intent)

			} catch (e: ActivityNotFoundException) {
				Toast.makeText(context, R.string.cannot_launch_activity, Toast.LENGTH_SHORT).show()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		},
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.tertiaryContainer,
			contentColor = MaterialTheme.colorScheme.onTertiaryContainer
		),
		shape = MaterialTheme.shapes.medium,
	) {
		Row(
			modifier = Modifier.padding(all = dimensionResource(id = R.dimen.card_padding)),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_battery_slanted),
				contentDescription = stringResource(id = R.string.turn_off_optimization)
			)
			Spacer(modifier = Modifier.width(12.dp))
			Text(
				text = stringResource(id = R.string.turn_off_optimization),
				style = MaterialTheme.typography.labelMedium
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun IgnoreBatteryOptimizationCardPreview() = RecorderAppTheme {
	IgnoreBatteryOptimizationCard()
}