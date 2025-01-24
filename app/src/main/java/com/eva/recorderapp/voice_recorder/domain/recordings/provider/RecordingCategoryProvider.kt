package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import kotlinx.coroutines.flow.Flow

typealias RecordingCategoriesModels = List<RecordingCategoryModel>


/**
 * Provides access to recording categories.
 */
interface RecordingCategoryProvider {

	/**
	 * A flow of recording categories as a [Resource].
	 *
	 * @return A [Flow] emitting [Resource] objects containing either a
	 *         [RecordingCategoriesModels] on success or an [Exception] on failure.
	 */
	val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>

	/**
	 * A flow of recording categories with item counts as a [Resource].
	 *
	 * @return A [Flow] emitting [Resource] objects containing either a
	 *         [RecordingCategoriesModels] with item counts on success or an [Exception] on failure.
	 */
	val recordingCategoriesFlowWithItemCount: Flow<Resource<RecordingCategoriesModels, Exception>>

	/**
	 * Retrieves a recording category by its ID.
	 *
	 * @param id The ID of the category to retrieve.
	 * @return A [Resource] containing the [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception>

	/**
	 * Creates a new recording category with the given name.
	 *
	 * @param name The name of the new category.
	 * @return A [Resource] containing the created [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception>

	/**
	 * Creates a new recording category with the given name, color, and type.
	 *
	 * @param name The name of the new category.
	 * @param color The [CategoryColor] of the new category.
	 * @param type The [CategoryType] of the new category.
	 * @return A [Resource] containing the created [RecordingCategoryModel] on success or an [Exception] on failure.
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
	 * @return A [Resource] containing the updated [RecordingCategoryModel] on success or an [Exception] on failure.
	 */
	suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception>

	/**
	 * Deletes a recording category.
	 *
	 * @param category The [RecordingCategoryModel] to delete.
	 * @return A [Resource] containing `true` on successful deletion or an [Exception] on failure.
	 */
	suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception>

	/**
	 * Deletes multiple recording categories.
	 *
	 * @param categories A collection of [RecordingCategoryModel] objects to delete.
	 * @return A [Resource] containing `true` on successful deletion of all categories or an [Exception] on failure.
	 */
	suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception>
}