package com.eva.feature_recordings.recordings.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SortOptions(@StringRes private val res: Int) {
	DATE_CREATED(R.string.sort_option_date_created),
	DURATION(R.string.sort_option_duration),
	NAME(R.string.sort_option_name),
	SIZE(R.string.sort_option_size);

	val strRes: String
		@Composable
		get() = stringResource(id = this.res)
}