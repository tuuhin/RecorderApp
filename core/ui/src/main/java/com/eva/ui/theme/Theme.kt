package com.eva.ui.theme

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.material3.DynamicMaterialExpressiveTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecorderAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        else -> null
    }

    if (colorScheme != null) {
        DynamicMaterialExpressiveTheme(
            seedColor = colorScheme.primary,
            primary = colorScheme.primary,
            secondary = colorScheme.secondary,
            tertiary = colorScheme.tertiary,
            error = colorScheme.error,
            isDark = darkTheme,
            motionScheme = MotionScheme.expressive(),
            style = AppPalette.paletteStyle,
            animate = true,
            animationSpec = tween(durationMillis = 300),
            content = content,
        )
    } else {
        DynamicMaterialExpressiveTheme(
            seedColor = AppPalette.SeedColor,
            isDark = darkTheme,
            motionScheme = MotionScheme.expressive(),
            style = AppPalette.paletteStyle,
            animate = true,
            animationSpec = tween(durationMillis = 300),
            content = content,
        )
    }
}
