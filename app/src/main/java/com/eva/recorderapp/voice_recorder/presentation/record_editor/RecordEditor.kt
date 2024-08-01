package com.eva.recorderapp.voice_recorder.presentation.record_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditor(
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = "Edit") },
				navigationIcon = navigation,
			)
		},
		modifier = modifier
	) { scPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(scPadding)
		) {

		}
	}
}