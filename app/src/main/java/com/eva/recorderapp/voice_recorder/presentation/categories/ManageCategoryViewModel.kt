package com.eva.recorderapp.voice_recorder.presentation.categories

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.CategoriesScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.SelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.toSelectableCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoryViewModel @Inject constructor(
	private val provider: RecordingCategoryProvider,
) : AppViewModel() {

	private val _categories = MutableStateFlow(emptyList<SelectableCategory>())
	val categories = _categories
		.map {
			it.filter { category -> category.category != RecordingCategoryModel.ALL_CATEGORY }
				.toImmutableList()
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(3000),
			initialValue = persistentListOf()
		)


	private val _isLoaded = MutableStateFlow(false)
	val isLoaded = _isLoaded.asStateFlow()

	private val selectedCategories: List<RecordingCategoryModel>
		get() = _categories.value.filter(SelectableCategory::isSelected)
			.map(SelectableCategory::category)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	init {
		// populate categories
		populateCategories()
	}

	fun onScreenEvent(event: CategoriesScreenEvent) {
		when (event) {
			CategoriesScreenEvent.OnSelectAll -> onSelectOrUnselectAll(true)
			CategoriesScreenEvent.OnUnSelectAll -> onSelectOrUnselectAll(false)
			CategoriesScreenEvent.OnDeleteSelected -> deleteCategories()
			is CategoriesScreenEvent.OnToggleSelection -> onToggleSelection(event.category)
		}
	}


	private fun onSelectOrUnselectAll(isSelected: Boolean) {
		_categories.update { categories ->
			categories.map { it.copy(isSelected = isSelected) }
		}
	}

	private fun onToggleSelection(categoryModel: RecordingCategoryModel) {
		_categories.update { categories ->
			categories.map { category ->
				if (category.category == categoryModel)
					category.copy(isSelected = !category.isSelected)
				else category
			}
		}
	}

	private fun populateCategories() {

		provider.recordingCategoryAsResourceFlow.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
					_isLoaded.update { true }
				}

				Resource.Loading -> _isLoaded.update { false }
				is Resource.Success -> {
					val new = res.data.toSelectableCategories()

					_categories.update { new }
					_isLoaded.update { true }
				}
			}
		}.launchIn(viewModelScope)
	}

	private fun deleteCategories() = viewModelScope.launch {
		when (val result = provider.deleteCategories(selectedCategories)) {
			is Resource.Error -> {
				val message = result.message ?: "Cannot delete category"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}

			is Resource.Success -> {
				val message = result.message ?: "Deleted categories successfully"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}
}