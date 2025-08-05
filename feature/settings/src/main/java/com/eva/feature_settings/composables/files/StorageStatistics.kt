package com.eva.feature_settings.composables.files

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recordings.domain.models.DeviceTotalStorageModel
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun StorageStatistics(
	model: DeviceTotalStorageModel,
	modifier: Modifier = Modifier,
	padding: PaddingValues = PaddingValues(16.dp),
) {
	val context = LocalContext.current

	val usedSpaceReadable = remember(model) {
		Formatter.formatFileSize(context, model.usedSpaceInBytes)
	}

	val totalSpaceReadable = remember(model) {
		Formatter.formatFileSize(context, model.totalAmountInBytes)
	}


	Column(
		modifier = modifier.padding(padding),
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		Text(
			text = stringResource(id = R.string.storage_statistics),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onBackground
		)

		LinearProgressIndicator(
			progress = { model.usedSpacePercentage },
			color = MaterialTheme.colorScheme.onSecondaryContainer,
			trackColor = MaterialTheme.colorScheme.secondaryContainer,
			strokeCap = StrokeCap.Round,
			gapSize = 4.dp,
			modifier = Modifier.fillMaxWidth()
		)

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(id = R.string.storage_statistics_used),
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = usedSpaceReadable,
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.secondary
				)
			}
			Column(
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(id = R.string.storage_statistics_total),
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = totalSpaceReadable,
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.secondary
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun StorageStatisticsPreview() = RecorderAppTheme {
	Surface {
		StorageStatistics(
			model = DeviceTotalStorageModel(
				totalAmountInBytes = 128 * 1024 * 1024,
				freeAmountInBytes = 20 * 1024 * 1024
			)
		)
	}
}