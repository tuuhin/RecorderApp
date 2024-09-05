package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.CreateOrEditCategoryEvent
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.CreateOrEditCategoryState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditCategorySheet(
	createOrEditState: CreateOrEditCategoryState,
	onEvent: (CreateOrEditCategoryEvent) -> Unit,
	modifier: Modifier = Modifier,
	sheetState: SheetState = rememberModalBottomSheetState()
) {
	val scope = rememberCoroutineScope()

	if (createOrEditState.showSheet)
		ModalBottomSheet(
			onDismissRequest = { onEvent(CreateOrEditCategoryEvent.OnDismissSheet) },
			sheetState = sheetState,
			tonalElevation = 2.dp,
			modifier = modifier,
		) {
			CreateOrEditCategorySheetContent(
				value = createOrEditState.textValue,
				error = createOrEditState.error,
				hasError = createOrEditState.hasError,
				isEditMode = createOrEditState.isEditMode,
				onValueChange = { onEvent(CreateOrEditCategoryEvent.OnTextFieldValueChange(it)) },
				onAcceptChanges = { onEvent(CreateOrEditCategoryEvent.OnAcceptChanges) },
				onCancel = {
					scope.launch { sheetState.hide() }
						.invokeOnCompletion {
							onEvent(CreateOrEditCategoryEvent.OnDismissSheet)
						}
				}
			)
		}
}


@Composable
private fun CreateOrEditCategorySheetContent(
	value: TextFieldValue,
	onValueChange: (TextFieldValue) -> Unit,
	onAcceptChanges: () -> Unit,
	onCancel: () -> Unit,
	modifier: Modifier = Modifier,
	error: String = "",
	hasError: Boolean = false,
	isEditMode: Boolean = false,
	padding: PaddingValues = PaddingValues(24.dp)
) {
	Column(
		modifier = modifier.padding(padding)
	) {
		Text(
			text = stringResource(id = R.string.create_new_category),
			style = MaterialTheme.typography.headlineSmall,
			color = AlertDialogDefaults.titleContentColor,
		)
		Text(
			text = stringResource(id = R.string.create_new_category_text),
			color = AlertDialogDefaults.textContentColor,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(vertical = 6.dp),
		)
		OutlinedTextField(
			value = value,
			onValueChange = onValueChange,
			label = { Text(text = stringResource(id = R.string.rename_label_text)) },
			isError = hasError,
			shape = MaterialTheme.shapes.large,
			keyboardActions = KeyboardActions(onDone = { onAcceptChanges() }),
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.Text,
				imeAction = ImeAction.Done
			),
			modifier = Modifier.fillMaxWidth()
		)
		AnimatedVisibility(
			visible = hasError,
			enter = slideInVertically(),
			exit = slideOutVertically()
		) {
			Text(text = error, style = MaterialTheme.typography.labelMedium)
		}
		Spacer(modifier = Modifier.height(12.dp))
		Row(
			modifier = Modifier.align(Alignment.End),
			horizontalArrangement = Arrangement.spacedBy(6.dp)
		) {
			TextButton(onClick = onCancel) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
			Button(onClick = onAcceptChanges) {
				Text(
					text = if (isEditMode) stringResource(R.string.menu_option_edit)
					else stringResource(id = R.string.menu_option_create)
				)
			}
		}
	}
}
