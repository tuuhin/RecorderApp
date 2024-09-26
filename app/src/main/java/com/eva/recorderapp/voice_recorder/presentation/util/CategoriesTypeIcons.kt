package com.eva.recorderapp.voice_recorder.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType

val CategoryType.imageVector: Painter
	@Composable
	get() = when (this) {
		CategoryType.CATEGORY_ICON_LABEL -> painterResource(R.drawable.ic_category_label)
		CategoryType.CATEGORY_ICON_IMPORTANT -> painterResource(R.drawable.ic_category_important)
		CategoryType.CATEGORY_WORK -> painterResource(R.drawable.ic_category_work)
		CategoryType.CATEGORY_MUSIC -> painterResource(R.drawable.ic_category_music)
		CategoryType.CATEGORY_GROUP -> painterResource(R.drawable.ic_category_group)
		CategoryType.CATEGORY_SONG -> painterResource(R.drawable.ic_category_song)
		CategoryType.CATEGORY_STUDY -> painterResource(R.drawable.ic_category_study)
		CategoryType.CATEGORY_MESSAGE -> painterResource(R.drawable.ic_category_message)
		CategoryType.CATEGORY_NONE -> painterResource(R.drawable.ic_folder)
	}
