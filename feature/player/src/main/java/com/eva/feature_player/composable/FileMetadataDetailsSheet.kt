package com.eva.feature_player.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.eva.player_shared.state.ContentLoadState
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileMetadataDetailsSheet(
	contentLoadState: ContentLoadState<AudioFileModel>,
	showBottomSheet: Boolean,
	onSheetDismiss: () -> Unit,
	modifier: Modifier = Modifier,
	sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
	if (!showBottomSheet || contentLoadState !is ContentLoadState.Content) return
	val fileModel = contentLoadState.data

	ModalBottomSheet(
		sheetState = sheetState,
		onDismissRequest = onSheetDismiss,
		modifier = modifier,
	) {
		FileMetaDataSheetContent(
			audio = fileModel,
			contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.bottom_sheet_padding_lg))
		)
	}
}