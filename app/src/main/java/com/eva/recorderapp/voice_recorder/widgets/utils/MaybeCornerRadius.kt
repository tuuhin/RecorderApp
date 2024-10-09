package com.eva.recorderapp.voice_recorder.widgets.utils

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background

fun GlanceModifier.maybeCornerRadius(cornerRadius: Dp, @DrawableRes resId: Int)
		: GlanceModifier {
	val modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		cornerRadius(cornerRadius)
	else GlanceModifier.background(ImageProvider(resId))
	return then(modifier)
}

fun GlanceModifier.maybeCornerRadius(@DrawableRes resId: Int)
		: GlanceModifier =
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GlanceModifier
	else then(GlanceModifier.background(ImageProvider(resId)))