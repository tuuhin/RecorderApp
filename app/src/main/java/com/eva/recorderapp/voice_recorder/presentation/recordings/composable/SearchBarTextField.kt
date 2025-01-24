package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarTextField(
	query: String,
	onQueryChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	onSearch: (String) -> Unit = {},
	onVoiceInput: (List<String>) -> Unit = {},
	colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
) {

	val context = LocalContext.current
	val updatedOnVoiceInput by rememberUpdatedState(onVoiceInput)

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult(),
		onResult = { result ->
			val message = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
				?.filterNotNull() ?: emptyList()
			updatedOnVoiceInput(message)
		}
	)

	val hasPermission = remember(context) {
		ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.RECORD_AUDIO
		) == PermissionChecker.PERMISSION_GRANTED
	}

	TextField(
		value = query,
		onValueChange = onQueryChange,
		placeholder = { Text(text = stringResource(R.string.voice_recognizer)) },
		trailingIcon = {
			TooltipBox(
				positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
				tooltip = {
					PlainTooltip {
						Text(text = stringResource(R.string.search_filter_title))
					}
				},
				state = rememberTooltipState(),
			) {
				IconButton(
					onClick = {
						val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
							putExtra(
								RecognizerIntent.EXTRA_LANGUAGE_MODEL,
								RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
							)
							putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
							putExtra(
								RecognizerIntent.EXTRA_LANGUAGE_MODEL,
								RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
							)
							putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
							putExtra(
								RecognizerIntent.EXTRA_PROMPT,
								context.getString(R.string.voice_search_prompt)
							)
						}
						try {
							launcher.launch(intent)
						} catch (e: Exception) {
							e.printStackTrace()
						}
					},
					modifier = Modifier.offset(x = (-4).dp),
					enabled = hasPermission
				) {
					Icon(
						imageVector = Icons.Outlined.MicNone,
						contentDescription = stringResource(R.string.voice_recognizer)
					)
				}
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
			disabledIndicatorColor = Color.Transparent,
			focusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
			errorTrailingIconColor = MaterialTheme.colorScheme.secondary
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