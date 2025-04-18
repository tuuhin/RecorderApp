package com.eva.categories.domain.provider

import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias RecordingCategoriesModels = List<RecordingCategoryModel>

/**
 * Provides access to recording categories.
 */
interface RecordingCategoryProvider {

	/**
	 * A flow of recording categories as a [Resource].
	 *
	 * @return A [kotlinx.coroutines.flow.Flow] emitting [com.eva.utils.Resource] objects containing either a
	 *         [RecordingCategoriesModels] on success or an [Exception] on failure.
	 */
	val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>

	/**
	 * A flow of recording categories with item counts as a [com.eva.utils.Resource].
	 *
	 * @return A [kotlinx.coroutines.flow.Flow] emitting [com.eva.utils.Resource] objects containing either a
	 *         [RecordingCategoriesModels] with item counts on success or an [Exception] on failure.
	 */
	val recordingCategoriesFlowWithItemCount: Flow<Resource<RecordingCategoriesModels, Exception>>

	/**
	 * Retrieves a recording category by its ID.
	 *
	 * @param id The ID of the category to retrieve.
	 * @return A [com.eva.utils.Resource] containing the [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception>

	/**
	 * Creates a new recording category with the given name.
	 *
	 * @param name The name of the new category.
	 * @return A [com.eva.utils.Resource] containing the created [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception>

	/**
	 * Creates a new recording category with the given name, color, and type.
	 *
	 * @param name The name of the new category.
	 * @param color The [CategoryColor] of the new category.
	 * @param type The [CategoryType] of the new category.
	 * @return A [com.eva.utils.Resource] containing the created [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun createCategory(
		name: String,
		color: CategoryColor,
		type: CategoryType,
	): Resource<RecordingCategoryModel, Exception>

	/**
	 * Updates an existing recording category.
	 *
	 * @param category The [RecordingCategoryModel] to update.
	 * @return A [com.eva.utils.Resource] containing the updated [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception>

	/**
	 * Deletes a recording category.
	 *
	 * @param category The [RecordingCategoryModel] to delete.
	 * @return A [com.eva.utils.Resource] containing `true` on successful deletion or an [Exception] on failure.
	 */
	suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception>

	/**
	 * Deletes multiple recording categories.
	 *
	 * @param categories A collection of [RecordingCategoryModel] objects to delete.
	 * @return A [com.eva.utils.Resource] containing `true` on successful deletion of all categories or an [Exception] on failure.
	 */
	suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception>
}