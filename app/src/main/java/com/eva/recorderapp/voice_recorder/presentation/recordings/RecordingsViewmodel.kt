package com.eva.recorderapp.voice_recorder.presentation.recordings

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.use_cases.RecordingsFromCategoriesUseCase
import com.eva.recorderapp.voice_recorder.domain.util.ShareRecordingsUtil
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RecordingsSortInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.sortedResults
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.toSelectableRecordings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewmodel @Inject constructor(
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
			started = SharingStarted.WhileSubscribed(3000),
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
			.map(SelectableRecordings::recoding)

	private val _deleteEvents = MutableSharedFlow<DeleteOrTrashRecordingsRequest>()
	val trashRequestEvent: SharedFlow<DeleteOrTrashRecordingsRequest>
		get() = _deleteEvents.asSharedFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private val _trashItemsFallback = MutableStateFlow(emptyList<RecordedVoiceModel>())
	private var _prepareRecordingsJob: Job? = null

	private fun populateRecordings() {
		// recordings are already loaded no need to again add a collector
		if (isLoaded.value) return
		// prepare the categories
		populateRecordingCategories()
		// populate the records according to requirements
		_selectedCategory.onEach(::populateRecords).launchIn(viewModelScope)
	}

	private fun populateRecords(categoryModel: RecordingCategoryModel?) {
		// cancels the job this the previous collector get cancelled
		_prepareRecordingsJob?.cancel()
		// set it to the new job
		_prepareRecordingsJob = viewModelScope.launch {
			recordingsFromCategoriesUseCase(categoryModel).onStart {
				// set the loading spinner
				_isRecordingsLoaded.update { false }
			}.catch { err -> err.printStackTrace() }.onEach { res ->
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
						_isRecordingsLoaded.update { true }
					}

					is Resource.Success -> {
						val new = res.data.toSelectableRecordings()
						_recordings.update { new }
						_isRecordingsLoaded.update { true }
					}
				}
			}.launchIn(this)
		}
	}

	private fun populateRecordingCategories() {
		// load all categories with count
		categoriesProvider.recordingCategoriesFlowWithItemCount
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "SOME ERROR"
						_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
						_isCategoriesLoaded.update { true }
					}

					Resource.Loading -> _isCategoriesLoaded.update { false }
					is Resource.Success -> {
						val data = res.data + RecordingCategoryModel.ALL_CATEGORY
						_categories.update { data.reversed() }
						_isCategoriesLoaded.update { true }
					}
				}
			}.launchIn(viewModelScope)
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
			is RecordingScreenEvent.OnPostTrashRequestApi30 -> onPostTrashEvent(
				event.isSuccess,
				event.message
			)

			RecordingScreenEvent.OnToggleFavourites -> onToggleFavourites()
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_recordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}


	private fun onTrashSelectedRecordings() = viewModelScope.launch {
		// set the trash items fallback
		_trashItemsFallback.update { selectedRecordings }
		// request for trash item
		when (val result = trashProvider.createTrashRecordings(selectedRecordings)) {
			is Resource.Error -> {
				if (result.error is SecurityException) {
					handleSecurityExceptionToTrash(result.error)
					return@launch
				}
				val message = result.message ?: "Cannot move items to trash"
				onPostTrashEvent(false, message)
			}

			is Resource.Success -> {
				val message = result.message ?: "Moved items to trash"
				onPostTrashEvent(true, message)
			}

			else -> {}
		}
	}


	private fun onToggleFavourites() = viewModelScope.launch {
		val isAllSelectedFavourite = selectedRecordings.all { it.isFavorite }

		val result = if (isAllSelectedFavourite) {
			val selection = selectedRecordings.map { record -> record.copy(isFavorite = false) }
			// unset favourite
			secondaryDataProvider.favouriteRecordingsBulk(selection, false)
		} else {
			val favouriteSelection = selectedRecordings.filterNot { it.isFavorite }
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


	private fun handleSecurityExceptionToTrash(error: SecurityException) = viewModelScope.launch {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

			val request = DeleteOrTrashRecordingsRequest.OnTrashRequest(selectedRecordings)
			_deleteEvents.emit(request)

		} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
			// TODO: Check the workflow for android 10
			(error as? RecoverableSecurityException)?.let { exp ->
				val pendingIntent = exp.userAction.actionIntent
				val request = IntentSenderRequest.Builder(pendingIntent).build()
				val trashEvent = DeleteOrTrashRecordingsRequest.OnTrashRequest(
					recordings = selectedRecordings, intentSenderRequest = request
				)
				_deleteEvents.emit(trashEvent)
			}
		}
	}


	private fun onPostTrashEvent(isSuccess: Boolean, message: String) = viewModelScope.launch {
		// if trash items are empty then no need to process trash events
		if (_trashItemsFallback.value.isEmpty()) return@launch
		// show the toast its deleted
		_uiEvents.emit(UIEvents.ShowToast(message))
		if (isSuccess) {
			val deletedFiles = _trashItemsFallback.value
			trashProvider.onPostTrashRecordings(deletedFiles)
		}
		//clear the deleted recordings list
		_trashItemsFallback.update { emptyList() }
	}

	private fun toggleRecordingSelection(recording: RecordedVoiceModel) {
		_recordings.update { recordings ->
			recordings.map { record ->
				if (record.recoding == recording)
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