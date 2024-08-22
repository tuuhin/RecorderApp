package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R

enum class SortOptions(@StringRes private val res: Int) {
	DATE_CREATED(R.string.sort_option_date_created),
	DURATION(R.string.sort_option_duration),
	NAME(R.string.sort_option_name),
	SIZE(R.string.sort_option_size);

	val strRes: String
		@Composable
		get() = stringResource(id = this.res)
}