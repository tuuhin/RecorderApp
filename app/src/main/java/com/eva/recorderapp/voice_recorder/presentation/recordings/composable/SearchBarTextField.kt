package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarTextField(
	query: String,
	onQueryChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	onSearch: (String) -> Unit = {},
	colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
) {
	TextField(
		value = query,
		onValueChange = onQueryChange,
		placeholder = { Text(text = stringResource(R.string.search_bar_placeholder)) },
		trailingIcon = {
			IconButton(onClick = {}, enabled = false) {
				Icon(
					imageVector = Icons.Outlined.MicNone,
					contentDescription = stringResource(R.string.mic_input)
				)
			}
		},
		singleLine = true,
		shape = SearchBarDefaults.inputFieldShape,
		visualTransformation = VisualTransformation.None,
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
		keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
		colors = colors.copy(
			unfocusedIndicatorColor = Color.Transparent,
			focusedIndicatorColor = Color.Transparent,
			errorIndicatorColor = Color.Transparent,
			disabledIndicatorColor = Color.Transparent
		),
		modifier = modifier
			.focusable()
			.sizeIn(
				minWidth = 360.dp,
				maxWidth = 720.dp,
				minHeight = SearchBarDefaults.InputFieldHeight
			)
	)
}