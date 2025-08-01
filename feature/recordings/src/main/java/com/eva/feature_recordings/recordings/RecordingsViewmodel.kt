package com.eva.feature_recordings.recordings

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.viewModelScope
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.feature_recordings.recordings.state.RecordingScreenEvent
import com.eva.feature_recordings.recordings.state.RecordingsSortInfo
import com.eva.feature_recordings.recordings.state.SelectableRecordings
import com.eva.feature_recordings.recordings.state.sortedResults
import com.eva.feature_recordings.recordings.state.toSelectableRecordings
import com.eva.feature_recordings.util.DeleteOrTrashRequestEvent
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.use_case.usecases.RecordingsFromCategoriesUseCase
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecordingsViewmodel @Inject constructor(
	private val recordingsFromCategoriesUseCase: RecordingsFromCategoriesUseCase,
	private val trashProvider: TrashRecordingsProvider,
	private val categoriesProvider: RecordingCategoryProvider,
	private val secondaryDataProvider: RecordingsSecondaryDataProvider,
	private val shareUtils: ShareRecordingsUtil,
) : AppViewModel() {

	private val _sortInfo = MutableStateFlow(RecordingsSortInfo())
	val sortInfo = _sortInfo.asStateFlow()

	private val _selectedCategory = MutableStateFlow(RecordingCategoryModel.ALL_CATEGORY)
	val selectedCategory = _selectedCategory.asStateFlow()

	private val _recordings = MutableStateFlow(emptyList<SelectableRecordings>())
	val recordings = combine(_recordings, _sortInfo, transform = ::sortedResults)
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val _categories = MutableStateFlow(emptyList<RecordingCategoryModel>())
	val categories = _categories
		.map { categories -> categories.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val _isRecordingsLoaded = MutableStateFlow(false)
	private val _isCategoriesLoaded = MutableStateFlow(false)

	val isLoaded = combine(_isRecordingsLoaded, _isCategoriesLoaded) { isRecordings, isCategories ->
		isRecordings && isCategories
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = false
	)

	private val selectedRecordings: List<RecordedVoiceModel>
		get() = _recordings.value.filter(SelectableRecordings::isSelected)
			.map(SelectableRecordings::recording)

	private val _trashRecordingEvent = MutableSharedFlow<DeleteOrTrashRequestEvent>()
	val trashRequestEvent: SharedFlow<DeleteOrTrashRequestEvent>
		get() = _trashRecordingEvent.asSharedFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private fun populateRecordings() {
		if (isLoaded.value) return
		// prepare the categories on a different coroutine
		viewModelScope.launch { populateRecordingCategories() }
		// populate the records according to requirements
		viewModelScope.launch { readRecordingsFromCategory() }
	}

	// Load all the recordings categories
	private suspend fun populateRecordingCategories() {
		categoriesProvider.recordingCategoryAsResourceFlow.collect { res ->
			when (res) {
				Resource.Loading -> _isCategoriesLoaded.update { false }
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
				}

				is Resource.Success -> _categories.update { res.data }
			}
			_isCategoriesLoaded.update { true }
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private suspend fun readRecordingsFromCategory() {
		_selectedCategory.flatMapLatest(recordingsFromCategoriesUseCase::invoke)
			.collect { res ->
				when (res) {
					Resource.Loading -> _isRecordingsLoaded.update { false }
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "SOME ERROR"
						_uiEvents.emit(
							UIEvents.ShowSnackBarWithActions(
								message = message,
								actionText = "Retry",
								action = ::populateRecordings
							)
						)
					}

					is Resource.Success -> _recordings.update { res.data.toSelectableRecordings() }
				}
				_isRecordingsLoaded.update { true }
			}
	}


	fun onScreenEvent(event: RecordingScreenEvent) {
		when (event) {
			is RecordingScreenEvent.OnRecordingSelectOrUnSelect -> toggleRecordingSelection(event.recording)
			RecordingScreenEvent.OnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(true)
			RecordingScreenEvent.OnUnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(false)
			RecordingScreenEvent.OnSelectedItemTrashRequest -> onTrashSelectedRecordings()
			is RecordingScreenEvent.OnSortOptionChange -> _sortInfo.update { it.copy(options = event.sort) }
			is RecordingScreenEvent.OnSortOrderChange -> _sortInfo.update { it.copy(order = event.order) }
			RecordingScreenEvent.ShareSelectedRecordings -> shareSelectedRecordings()
			RecordingScreenEvent.PopulateRecordings -> populateRecordings()
			is RecordingScreenEvent.OnCategoryChanged -> _selectedCategory.update { event.categoryModel }
			RecordingScreenEvent.OnToggleFavourites -> onToggleFavourites()
			is RecordingScreenEvent.OnPostTrashRequest -> viewModelScope.launch {
				_uiEvents.emit(UIEvents.ShowToast(event.message))
			}
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_recordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}

	private fun onTrashSelectedRecordings() {
		// request for trash item
		trashProvider.createTrashRecordings(selectedRecordings)
			.onEach { result ->
				when (result) {
					is Resource.Error -> {
						val error = result.error
						if (error is SecurityException) {
							// on security exception handle the case
							handleSecurityExceptionToTrash(error, result.data)
							return@onEach
						}
						val message = result.error.message ?: result.message
						?: "Cannot move items to trash"

						viewModelScope.launch {
							_uiEvents.emit(UIEvents.ShowSnackBar(message))
						}
					}

					is Resource.Success -> {
						val message = result.message ?: "Moved items to trash"
						viewModelScope.launch {
							_uiEvents.emit(UIEvents.ShowToast(message))
						}
					}

					else -> {}
				}
			}
			.onCompletion { onSelectOrUnSelectAllRecordings(false) }
			.launchIn(viewModelScope)
	}


	private fun onToggleFavourites() = viewModelScope.launch {
		val isAllSelectedFavourite = selectedRecordings.all(RecordedVoiceModel::isFavorite)

		val result = if (isAllSelectedFavourite) {
			val selection = selectedRecordings.map { record -> record.copy(isFavorite = false) }
			// unset favourite
			secondaryDataProvider.favouriteRecordingsBulk(selection, false)
		} else {
			val favouriteSelection = selectedRecordings.filterNot(RecordedVoiceModel::isFavorite)
				.map { record -> record.copy(isFavorite = true) }
			// set favourite
			secondaryDataProvider.favouriteRecordingsBulk(favouriteSelection, true)
		}

		when (result) {
			is Resource.Error -> {
				val message = result.message ?: result.error.message ?: "Error"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}

			is Resource.Success -> {
				val message = result.message ?: "Added to Favourites"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}

			else -> {}
		}
	}


	private fun handleSecurityExceptionToTrash(
		error: SecurityException,
		recordingsToTrash: Collection<RecordedVoiceModel>? = null,
	) {
		if (recordingsToTrash == null) return

		viewModelScope.launch {
			val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				DeleteOrTrashRequestEvent.OnTrashRequest(recordingsToTrash)
			} else {
				if (error !is RecoverableSecurityException) return@launch
				val pendingIntent = error.userAction.actionIntent
				val senderRequest = IntentSenderRequest.Builder(pendingIntent).build()

				DeleteOrTrashRequestEvent.OnTrashRequest(
					recordings = recordingsToTrash,
					intentSenderRequest = senderRequest
				)
			}
			_trashRecordingEvent.emit(request)
		}
	}

	private fun toggleRecordingSelection(recording: RecordedVoiceModel) {
		_recordings.update { recordings ->
			recordings.map { record ->
				if (record.recording == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}

	private fun shareSelectedRecordings() = viewModelScope.launch {
		when (val result = shareUtils.shareAudioFiles(selectedRecordings)) {
			is Resource.Error -> {
				val message = result.message ?: "SOME ERROR SHARING RECORDING"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}
}