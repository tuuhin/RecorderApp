package com.eva.feature_recordings.recordings.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SortOrder(@StringRes private val res: Int) {
	ASC(R.string.sort_order_asc),
	DESC(R.string.sort_order_desc);

	val strResource: String
		@Composable
		get() = stringResource(id = res)
}