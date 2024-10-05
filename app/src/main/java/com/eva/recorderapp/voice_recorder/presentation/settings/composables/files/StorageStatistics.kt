package com.eva.recorderapp.voice_recorder.presentation.settings.composables.files

import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun StorageStatistics(
	modifier: Modifier = Modifier,
	padding: PaddingValues = PaddingValues(16.dp),
) {
	val context = LocalContext.current
	val isInspectionMode = LocalInspectionMode.current

	val statsManager = remember {
		if (isInspectionMode) return@remember null
		context.getSystemService<StorageStatsManager>()
	}

	val freeSpace = remember(statsManager) {
		statsManager?.getFreeBytes(StorageManager.UUID_DEFAULT) ?: 0L
	}

	val freeSpaceReadable = remember(freeSpace) {
		Formatter.formatFileSize(context, freeSpace)
	}

	val totalSpace = remember(statsManager) {
		statsManager?.getTotalBytes(StorageManager.UUID_DEFAULT) ?: 0L
	}

	val totalSpaceReadable = remember(totalSpace) {
		Formatter.formatFileSize(context, totalSpace)
	}

	val ratio by remember(freeSpace, totalSpace) {
		derivedStateOf {
			if (isInspectionMode) return@derivedStateOf 0.5f
			else if (totalSpace == 0L) return@derivedStateOf 0f
			else 1 - (freeSpace / totalSpace).toFloat()
		}
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
			progress = { ratio },
			color = MaterialTheme.colorScheme.onSecondaryContainer,
			trackColor = MaterialTheme.colorScheme.secondaryContainer,
			strokeCap = StrokeCap.Round,
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
					text = freeSpaceReadable,
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
		StorageStatistics()
	}
}