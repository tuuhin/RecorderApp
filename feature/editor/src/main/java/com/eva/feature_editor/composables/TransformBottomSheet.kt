package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.TransformationProgress
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.feature_editor.event.TransformationState
import com.eva.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransformBottomSheet(
	onDismiss: () -> Unit,
	onTransform: () -> Unit,
	onExport: () -> Unit,
	modifier: Modifier = Modifier,
	showSheet: Boolean = true,
	state: TransformationState = TransformationState(),
	sheetState: SheetState = rememberModalBottomSheetState()
) {
	if (!showSheet) return

	ModalBottomSheet(
		onDismissRequest = { if (!state.isTransforming) onDismiss() },
		sheetState = sheetState,
		modifier = modifier,
	) {
		Column(
			modifier = Modifier
				.padding(dimensionResource(R.dimen.bottom_sheet_padding_lg))
				.fillMaxWidth()
				.defaultMinSize(minHeight = 200.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Bottom,
		) {
			val text = when (state.progress) {
				TransformationProgress.Idle -> "IDLE"
				is TransformationProgress.Progress -> "${state.progress.amount}"
				TransformationProgress.UnAvailable -> "Not ready"
				TransformationProgress.Waiting -> "Waiting.."
			}

			Text(text = text)

			Button(
				onClick = if (state.isExportFileReady) onExport else onTransform,
				shape = MaterialTheme.shapes.extraLarge,
				enabled = !state.isTransforming,
				modifier = Modifier.fillMaxWidth(),
				contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
			) {
				Text(text = "Export", style = MaterialTheme.typography.titleMedium)
			}
		}
	}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransformBottomSheet(
	state: TransformationState,
	onDismiss: () -> Unit,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	showSheet: Boolean = true,
	bottomSheetState: SheetState = rememberModalBottomSheetState()
) {
	TransformBottomSheet(
		onDismiss = {
			onEvent(EditorScreenEvent.OnDismissExportSheet)
			onDismiss()
		},
		showSheet = showSheet,
		sheetState = bottomSheetState,
		state = state,
		onTransform = { onEvent(EditorScreenEvent.BeginTransformation) },
		onExport = { onEvent(EditorScreenEvent.OnSaveExportFile) },
		modifier = modifier,
	)
}

