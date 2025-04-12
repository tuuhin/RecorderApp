package com.eva.feature_categories.create_category

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateCategoryViewModel @Inject constructor(
	private val categoryProvider: RecordingCategoryProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.CreateOrUpdateCategory
		get() = savedStateHandle.toRoute()

	private val _createState = MutableStateFlow(CreateCategoryState())
	val createState = _createState
		.onStart { loadCategory() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = CreateCategoryState()
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	fun onEvent(event: CreateCategoryEvent) {
		when (event) {
			is CreateCategoryEvent.OnTextFieldValueChange -> _createState.update { state ->
				state.copy(textValue = event.value)
			}

			is CreateCategoryEvent.OnCategoryColorSelect -> _createState.update { state ->
				state.copy(color = event.color)
			}

			is CreateCategoryEvent.OnCategoryTypeChange -> _createState.update { state ->
				state.copy(type = event.type)
			}

			CreateCategoryEvent.OnCreateOrEditCategory -> {
				if (!_createState.value.isEditMode) onCreateCategory()
				else onUpdateCategory()
			}
		}
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
				_createState.update { state ->
					state.copy(
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

		val model = state.toModel(categoryId)

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
			val result = categoryProvider.createCategory(categoryName, state.color, state.type)
			when (result) {
				is Resource.Error -> result.message?.let { message ->
					_createState.update { it.copy(error = message) }
				}

				is Resource.Success -> {
					_uiEvents.emit(UIEvents.PopScreen)
					result.message?.let { _uiEvents.emit(UIEvents.ShowToast(it)) }
				}

				else -> {}
			}
		}
	}
}