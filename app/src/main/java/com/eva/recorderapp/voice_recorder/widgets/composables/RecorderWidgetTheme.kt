package com.eva.recorderapp.voice_recorder.widgets.composables

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.material3.ColorProviders
import com.eva.recorderapp.ui.theme.darkScheme
import com.eva.recorderapp.ui.theme.lightScheme

@Composable
fun RecorderWidgetTheme(
	dynamicColor: Boolean = true,
	content: @Composable() () -> Unit,
) {
	val context = LocalContext.current

	val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor)
		ColorProviders(
			light = dynamicLightColorScheme(context),
			dark = dynamicDarkColorScheme(context)
		) else ColorProviders(light = lightScheme, dark = darkScheme)

	GlanceTheme(
		colors = colorScheme,
		content = content
	)
}