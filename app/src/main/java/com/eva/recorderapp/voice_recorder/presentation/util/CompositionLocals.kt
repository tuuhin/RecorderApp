package com.eva.recorderapp.voice_recorder.presentation.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackBarProvider = staticCompositionLocalOf { SnackbarHostState() }
