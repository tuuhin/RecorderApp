package com.eva.feature_categories

import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

object CategoriesPreviewFakes {
	val FAKE_CATEGORY_WITH_COLOR_AND_TYPE = RecordingCategoryModel(
		id = 0L,
		name = "Android",
		categoryType = CategoryType.CATEGORY_SONG,
		categoryColor = CategoryColor.COLOR_BLUE
	)

	val FAKE_CATEGORIES_WITH_ALL_OPTION: ImmutableList<RecordingCategoryModel>
		get() = (List(4) {
			RecordingCategoryModel(
				id = 0L,
				name = "Android",
				categoryType = CategoryType.entries.random(),
				categoryColor = CategoryColor.entries.random()
			)
		} + FAKE_CATEGORY_WITH_COLOR_AND_TYPE + RecordingCategoryModel.Companion.ALL_CATEGORY).reversed()
			.toImmutableList()

	private val FAKE_RECORDING_CATEGORY = RecordingCategoryModel(
		id = 0L, name = "Something", createdAt = LocalDateTime.now().toKotlinLocalDateTime()
	)

	val FAKE_RECORDING_CATEGORIES = List(10) { FAKE_RECORDING_CATEGORY }.toImmutableList()
}