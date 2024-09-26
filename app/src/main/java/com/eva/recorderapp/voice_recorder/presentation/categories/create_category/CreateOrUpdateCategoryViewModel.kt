package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateOrUpdateCategoryViewModel @Inject constructor(
	private val categoryProvider: RecordingCategoryProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.CreateOrUpdateCategory
		get() = savedStateHandle.toRoute()

	private val _createState = MutableStateFlow(CreateOrUpdateCategoryState())
	val createState = _createState.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	fun onEvent(event: CreateCategoryScreenEvents) {
		when (event) {
			is CreateCategoryScreenEvents.OnTextFieldValueChange -> _createState.update { state ->
				state.copy(textValue = event.value)
			}

			is CreateCategoryScreenEvents.OnCategoryColorSelect -> _createState.update { state ->
				state.copy(color = event.color)
			}

			is CreateCategoryScreenEvents.OnCategoryTypeChange -> _createState.update { state ->
				state.copy(type = event.type)
			}

			CreateCategoryScreenEvents.OnCreateOrEditCategory -> {
				if (!_createState.value.isEditMode) onCreateCategory()
				else onUpdateCategory()
			}
		}
	}

	init {
		loadCategory()
	}


	private fun loadCategory() = viewModelScope.launch {
		// no category id provided thus it's a creation
		val categoryId = route.categoryId ?: return@launch

		when (val result = categoryProvider.getCategoryFromId(categoryId)) {
			is Resource.Error -> {
				_uiEvents.emit(UIEvents.ShowSnackBar("Cannot Find a category Fallback to create"))
			}

			is Resource.Success -> {
				val category = result.data
				// update the fields
				_createState.update {
					CreateOrUpdateCategoryState(
						color = category.categoryColor,
						type = category.categoryType,
						textValue = TextFieldValue(text = category.name),
						isEditMode = true,
					)
				}
			}

			else -> {}
		}
	}

	private fun onUpdateCategory() {
		val categoryId = route.categoryId ?: return

		val state = _createState.value
		val categoryName = state.textValue.text

		if (categoryName.isBlank()) {
			_createState.update { it.copy(error = "Cannot have empty values") }
			return
		}
		val model = RecordingCategoryModel(
			id = categoryId,
			name = categoryName,
			categoryColor = state.color,
			categoryType = state.type
		)
		viewModelScope.launch {
			// show context message
			when (val result = categoryProvider.updateCategory(model)) {
				is Resource.Error -> {

					val message = result.message ?: "Cannot perform edit"
					val snackBarMessage = result.error.message ?: "Cannot update category"

					_createState.update { it.copy(error = message) }
					_uiEvents.emit(UIEvents.ShowSnackBar(snackBarMessage))
				}

				is Resource.Success -> {
					val message = result.message ?: "Updated $categoryName"
					_uiEvents.emit(UIEvents.PopScreen)
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				else -> {}
			}
		}
	}

	private fun onCreateCategory() {
		val state = _createState.value
		val categoryName = state.textValue.text

		if (categoryName.isBlank()) {
			_createState.update { it.copy(error = "Cannot have empty values") }
			return
		}
		viewModelScope.launch {
			// show context message
			val result = categoryProvider.createCategory(
				name = categoryName,
				color = state.color,
				type = state.type
			)

			when (result) {
				is Resource.Error -> {
					val message = result.message ?: "Failed to create category"
					_createState.update { it.copy(error = message) }
				}

				is Resource.Success -> {
					val message = result.message ?: "Created new category $categoryName"
					_uiEvents.emit(UIEvents.PopScreen)
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				else -> {}
			}
		}
	}
}