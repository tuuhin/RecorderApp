package com.eva.feature_categories.manage_category

import androidx.lifecycle.viewModelScope
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ManageCategoryViewModel @Inject constructor(
	private val provider: RecordingCategoryProvider,
) : AppViewModel() {

	private val _categories = MutableStateFlow(emptyList<RecordingCategoryModel>())
	val categories = _categories
		.map {
			it.filter { category -> category != RecordingCategoryModel.ALL_CATEGORY }
				.toImmutableList()
		}
		.onStart { populateCategories() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(3000),
			initialValue = persistentListOf()
		)


	private val _isLoaded = MutableStateFlow(false)
	val isLoaded = _isLoaded.asStateFlow()


	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onScreenEvent(event: ManageCategoriesEvent) {
		when (event) {
			is ManageCategoriesEvent.OnDeleteCategory -> deleteCategory(event.category)
		}
	}


	private fun populateCategories() {

		provider.recordingCategoryAsResourceFlow.onEach { res ->
			when (res) {
				Resource.Loading -> _isLoaded.update { false }
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
				}

				is Resource.Success -> {
					_categories.update { res.data }
				}
			}
			_isLoaded.update { true }
		}.launchIn(viewModelScope)
	}

	private fun deleteCategory(categoryModel: RecordingCategoryModel) = viewModelScope.launch {
		when (val result = provider.deleteCategory(categoryModel)) {
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