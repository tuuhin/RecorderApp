package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
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
	onCancelTransform: () -> Unit = {},
	showSheet: Boolean = true,
	state: TransformationState = TransformationState(),
	sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
	if (!showSheet) return

	ModalBottomSheet(
		onDismissRequest = { if (!state.isTransforming) onDismiss() },
		sheetState = sheetState,
		modifier = modifier,
		properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !state.isTransforming)
	) {
		TransformsSheetContent(
			state = state,
			onExport = onExport,
			onTransform = onTransform,
			onCancelTransform = onCancelTransform,
			contentPadding = PaddingValues(dimensionResource(R.dimen.bottom_sheet_padding_lg))
		)
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
		onCancelTransform = { onEvent(EditorScreenEvent.OnCancelTransformation) },
		modifier = modifier,
	)
}

