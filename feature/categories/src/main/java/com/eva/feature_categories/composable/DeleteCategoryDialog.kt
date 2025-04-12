package com.eva.feature_categories.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun DeleteCategoryDialog(
	showDialog: Boolean,
	onCancel: () -> Unit,
	onDeleteAfterWarn: () -> Unit,
	modifier: Modifier = Modifier,
) {
	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onCancel,
		confirmButton = {
			TextButton(onClick = onDeleteAfterWarn) {
				Text(text = stringResource(id = R.string.recording_action_delete))
			}
		},
		dismissButton = {
			TextButton(onClick = onCancel) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		},
		title = { Text(text = stringResource(id = R.string.delete_category_dialog_title)) },
		text = { Text(text = stringResource(id = R.string.delete_category_dialog_text)) },
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.ic_delete),
				contentDescription = stringResource(id = R.string.recording_action_trash)
			)
		},
		modifier = modifier,
	)
}

@PreviewLightDark
@Composable
private fun DeleteCategoryDialogPreview() = RecorderAppTheme {
	DeleteCategoryDialog(showDialog = true, onDeleteAfterWarn = {}, onCancel = {})
}