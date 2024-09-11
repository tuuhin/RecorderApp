package com.eva.recorderapp.voice_recorder.presentation.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.RecordingCategoryEvent
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
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
class SelectRecordingCategoryViewModel @Inject constructor(
	private val categoryProvider: RecordingCategoryProvider,
	private val dataProvider: RecordingsSecondaryDataProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.SelectRecordingCategoryRoute
		get() = savedStateHandle.toRoute()

	private val recordingIds: Collection<Long>
		get() = route.recordingIds

	private val _selected = MutableStateFlow<RecordingCategoryModel?>(null)
	val selectedCategory = _selected.asStateFlow()

	private val _categories = MutableStateFlow(emptyList<RecordingCategoryModel>())
	val categories = _categories
		.map { it.toImmutableList() }
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

	init {
		// populate categories
		populateCategories()
	}

	fun onEvent(event: RecordingCategoryEvent) {
		when (event) {
			is RecordingCategoryEvent.SelectCategory -> _selected.update { prev ->
				val new = event.category
				if (prev == new) null else new
			}

			RecordingCategoryEvent.OnSetRecordingCategory -> onSetRecordingCategory()
		}
	}

	private fun onSetRecordingCategory() = viewModelScope.launch {
		val selectedCategory = _selected.value ?: return@launch

		if (recordingIds.isEmpty()) {
			_uiEvents.emit(UIEvents.ShowToast(message = "Select recordings"))
			_uiEvents.emit(UIEvents.PopScreen)
			return@launch
		}

		val result = dataProvider.updateRecordingCategoryBulk(
			recordingIds = recordingIds.toList(),
			category = selectedCategory
		)

		when (result) {
			is Resource.Error -> {
				val message = result.message ?: result.error.message ?: "SOME ERROR"
				_uiEvents.emit(UIEvents.ShowToast(message = message))
			}

			is Resource.Success -> {
				val message = result.message ?: "Category set"
				_uiEvents.emit(UIEvents.ShowToast(message = message))
				_uiEvents.emit(UIEvents.PopScreen)
			}

			else -> {}
		}
	}

	private fun populateCategories() = categoryProvider.recordingCategoryAsResourceFlow
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(UIEvents.ShowToast(message = message))
					_uiEvents.emit(UIEvents.PopScreen)
					_isLoaded.update { true }
				}

				Resource.Loading -> _isLoaded.update { false }
				is Resource.Success -> {
					_categories.update { res.data }
					_isLoaded.update { true }
				}
			}
		}.launchIn(viewModelScope)

}