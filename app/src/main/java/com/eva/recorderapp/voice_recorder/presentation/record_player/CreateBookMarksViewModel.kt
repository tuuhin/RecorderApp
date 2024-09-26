package com.eva.recorderapp.voice_recorder.presentation.record_player

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.data.util.roundToClosestSeconds
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingBookmarksProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.BookMarkEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.CreateOrEditBookMarkState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
class CreateBookMarksViewModel @Inject constructor(
	private val bookmarksProvider: RecordingBookmarksProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	private val audioId: Long
		get() = route.audioId

	private val _createOrEditBookMarkState = MutableStateFlow(CreateOrEditBookMarkState())
	val bookmarkState = _createOrEditBookMarkState.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private val bookMarksTimeStamps: StateFlow<List<LocalTime>>
		get() = bookmarksProvider.getRecordingBookmarksFromId(audioId)
			.map { models -> models.map { it.timeStamp } }
			.stateIn(
				scope = viewModelScope,
				started = SharingStarted.Eagerly,
				initialValue = emptyList()
			)

	fun onBookMarkEvent(event: BookMarkEvents) {
		when (event) {
			is BookMarkEvents.OnDeleteBookmark -> onDeleteBookMark(event.bookmarkModel)
			is BookMarkEvents.OnUpdateTextField -> _createOrEditBookMarkState.update { state ->
				state.copy(textValue = event.textFieldValue)
			}

			BookMarkEvents.OnCloseDialog -> _createOrEditBookMarkState.update { state ->
				state.copy(showDialog = false, bookMarkModel = null)
			}

			BookMarkEvents.OpenDialogToCreate -> {
				_createOrEditBookMarkState.update { state ->
					state.copy(showDialog = true, bookMarkModel = null)
				}
			}

			is BookMarkEvents.OpenDialogToEdit -> {
				_createOrEditBookMarkState.update { state ->
					val bookMarkText = event.bookMark.text

					val range = if (bookMarkText.isNotBlank())
						TextRange(start = 0, end = bookMarkText.length)
					else TextRange.Zero

					state.copy(
						bookMarkModel = event.bookMark,
						showDialog = true,
						textValue = TextFieldValue(event.bookMark.text, selection = range)
					)
				}
			}

			is BookMarkEvents.OnAddOrUpdateBookMark -> {
				val state = _createOrEditBookMarkState.value
				if (state.isUpdate && state.bookMarkModel != null) {
					onUpdateBookMark(bookmark = state.bookMarkModel, state.textValue.text)
				} else {
					val bookMarkText = state.textValue.text.ifBlank { null }
					onAddBookMark(bookMarkText, event.time)
				}
			}
		}
	}

	private fun onAddBookMark(bookMarkText: String?, time: LocalTime) {

		viewModelScope.launch {
			// bookmarks should be lesser than recorderTime
			val bookMarkTime = time.roundToClosestSeconds()

			if (bookMarkTime in bookMarksTimeStamps.value) {
				return@launch
			}

			val result = if (bookMarkText != null) {
				bookmarksProvider.createBookMark(
					recordingId = audioId,
					time = bookMarkTime,
					text = bookMarkText,
				)
			} else {
				val collection = setOf(bookMarkTime)
				bookmarksProvider.createBookMarks(audioId, collection)
			}
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: "Cannot add bookmark"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success ->
					_createOrEditBookMarkState.update { CreateOrEditBookMarkState() }

				else -> {}
			}
		}
	}

	private fun onUpdateBookMark(bookmark: AudioBookmarkModel, text: String) {
		viewModelScope.launch {
			val bookmarkText = text.ifBlank { null }
			//show save toast
			when (val result = bookmarksProvider.updateBookMark(bookmark, bookmarkText)) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: "Cannot add bookmark"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success ->
					_createOrEditBookMarkState.update { CreateOrEditBookMarkState() }

				else -> {}
			}
		}
	}

	private fun onDeleteBookMark(bookmarkModel: AudioBookmarkModel) {
		viewModelScope.launch {
			// bookmarks should be lesser than recorderTime
			val bookMarkTime = listOf(bookmarkModel)
			val result = bookmarksProvider.deleteBookMarks(bookMarkTime)
			//show save toast
			(result as? Resource.Error)?.let { res ->
				val message = res.message ?: res.error.message ?: "Cannot add bookmark"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}
		}
	}
}