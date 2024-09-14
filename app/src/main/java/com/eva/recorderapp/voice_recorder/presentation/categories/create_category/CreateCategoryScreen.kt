package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditCategoryScreen(
	state: CreateOrUpdateCategoryState,
	onEvent: (CreateCategoryScreenEvents) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {
	Scaffold(
		topBar = {
			MediumTopAppBar(
				title = {
					if (state.isEditMode) Text(text = stringResource(R.string.edit_category_title))
					else Text(text = stringResource(id = R.string.create_category_title))
				},
				navigationIcon = navigation,
				actions = {
					TextButton(
						onClick = { onEvent(CreateCategoryScreenEvents.OnCreateOrEditCategory) }
					) {
						if (state.isEditMode) Text(text = stringResource(R.string.edit_category_action))
						else Text(text = stringResource(R.string.create_category_action))
					}
				},
			)
		},
		modifier = modifier,
	) { scPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(
					start = dimensionResource(id = R.dimen.sc_padding),
					end = dimensionResource(id = R.dimen.sc_padding),
					top = dimensionResource(id = R.dimen.sc_padding) + scPadding.calculateTopPadding(),
					bottom = dimensionResource(id = R.dimen.sc_padding) + scPadding.calculateBottomPadding()
				),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Text(
				text = stringResource(id = R.string.create_new_category_text),
				color = AlertDialogDefaults.textContentColor,
				style = MaterialTheme.typography.bodyMedium,
				modifier = Modifier.padding(vertical = 6.dp),
			)
			OutlinedTextField(
				value = state.textValue,
				onValueChange = { onEvent(CreateCategoryScreenEvents.OnTextFieldValueChange(it)) },
				label = { Text(text = stringResource(id = R.string.rename_label_text)) },
				isError = state.hasError,
				shape = MaterialTheme.shapes.medium,
				keyboardActions = KeyboardActions(onDone = { }),
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Text,
					imeAction = ImeAction.Done
				),
				modifier = Modifier.fillMaxWidth()
			)
			Spacer(modifier = Modifier.height(2.dp))
			AnimatedVisibility(
				visible = state.hasError,
				enter = slideInVertically(),
				exit = slideOutVertically()
			) {
				Text(
					text = state.error,
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.error
				)
			}
			Text(
				text = stringResource(R.string.category_color_title),
				style = MaterialTheme.typography.titleMedium
			)
			CategoryColorPicker(
				selectedColor = state.color,
				onColorChange = { onEvent(CreateCategoryScreenEvents.OnCategoryColorSelect(it)) })
			Text(
				text = stringResource(R.string.category_type_title),
				style = MaterialTheme.typography.titleMedium
			)
			CategoryTypePicker(
				selected = state.type,
				onSelectionChange = { onEvent(CreateCategoryScreenEvents.OnCategoryTypeChange(it)) })
		}
	}
}


@PreviewLightDark
@Composable
private fun CreateCategoryDialogContentPreview() = RecorderAppTheme {
	CreateOrEditCategoryScreen(
		state = CreateOrUpdateCategoryState(),
		onEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}