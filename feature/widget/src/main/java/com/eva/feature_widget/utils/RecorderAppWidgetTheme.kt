package com.eva.feature_widget.utils

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.material3.ColorProviders
import com.eva.ui.theme.AppPalette
import com.materialkolor.material3.rememberDynamicMaterialThemeState

@Composable
fun RecorderAppWidgetTheme(
    content: @Composable() () -> Unit,
) {
    val context = LocalContext.current

    val lightThemeState = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val colorScheme = dynamicLightColorScheme(context)
            rememberDynamicMaterialThemeState(
                seedColor = colorScheme.primary,
                primary = colorScheme.primary,
                secondary = colorScheme.secondary,
                tertiary = colorScheme.tertiary,
                error = colorScheme.error,
                isDark = false,
                style = AppPalette.paletteStyle,
            )
        }

        else -> rememberDynamicMaterialThemeState(
            seedColor = AppPalette.SeedColor,
            isDark = false,
            style = AppPalette.paletteStyle,
        )
    }

    val darkThemeState = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val colorScheme = dynamicDarkColorScheme(context)
            rememberDynamicMaterialThemeState(
                seedColor = colorScheme.primary,
                primary = colorScheme.primary,
                secondary = colorScheme.secondary,
                tertiary = colorScheme.tertiary,
                error = colorScheme.error,
                isDark = true,
                style = AppPalette.paletteStyle,
            )
        }

        else -> rememberDynamicMaterialThemeState(
            seedColor = AppPalette.SeedColor,
            isDark = true,
            style = AppPalette.paletteStyle,
        )
    }

    GlanceTheme(
        colors = ColorProviders(light = lightThemeState.colorScheme, dark = darkThemeState.colorScheme),
        content = content,
    )
}
