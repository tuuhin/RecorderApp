package com.eva.recorderapp.voice_recorder.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor

val CategoryColor.colorRes: Color?
	@Composable
	get() = when (this) {
		CategoryColor.COLOR_RED -> colorResource(R.color.tailwind_color_red)
		CategoryColor.COLOR_ORANGE -> colorResource(R.color.tailwind_color_orange)
		CategoryColor.COLOR_YELLOW -> colorResource(R.color.tailwind_color_yellow)
		CategoryColor.COLOR_AMBER -> colorResource(R.color.tailwind_color_amber)
		CategoryColor.COLOR_LIME -> colorResource(R.color.tailwind_color_lime)
		CategoryColor.COLOR_GREEN -> colorResource(R.color.tailwind_color_green)
		CategoryColor.COLOR_EMERALD -> colorResource(R.color.tailwind_color_emerald)
		CategoryColor.COLOR_TEAL -> colorResource(R.color.tailwind_color_teal)
		CategoryColor.COLOR_CYAN -> colorResource(R.color.tailwind_color_cyan)
		CategoryColor.COLOR_SKY -> colorResource(R.color.tailwind_color_sky)
		CategoryColor.COLOR_BLUE -> colorResource(R.color.tailwind_color_blue)
		CategoryColor.COLOR_INDIGO -> colorResource(R.color.tailwind_color_indigo)
		CategoryColor.COLOR_VIOLET -> colorResource(R.color.tailwind_color_violet)
		CategoryColor.COLOR_PURPLE -> colorResource(R.color.tailwind_color_purple)
		CategoryColor.COLOR_PINK -> colorResource(R.color.tailwind_color_pink)
		CategoryColor.COLOR_ROSE -> colorResource(R.color.tailwind_color_rose)
		CategoryColor.COLOR_UNKNOWN -> null
	}