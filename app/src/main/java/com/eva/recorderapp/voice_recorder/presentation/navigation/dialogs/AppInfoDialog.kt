package com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.voice_recorder.presentation.composables.AppDialogInfoContent
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs

fun NavGraphBuilder.appInfoDialog() = dialog<NavDialogs.ApplicationInfo> {
	AppDialogInfoContent()
}