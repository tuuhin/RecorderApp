package com.eva.feature_player.bookmarks

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.feature_player.bookmarks.state.BookMarkEvents
import com.eva.feature_player.bookmarks.state.CreateBookmarkState
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import com.eva.utils.roundToClosestSeconds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
internal class BookMarksViewModel @Inject constructor(
	private val bookmarksProvider: RecordingBookmarksProvider,
	private val sharingUtil: ShareRecordingsUtil,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	private val audioId: Long
		get() = route.audioId

	private val _createOrEditBookMarkState = MutableStateFlow(CreateBookmarkState())
	val bookmarkState = _createOrEditBookMarkState.asStateFlow()

	val bookMarksFlow = bookmarksProvider.getRecordingBookmarksFromId(audioId)
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Companion.Eagerly,
			initialValue = persistentListOf()
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


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
					else TextRange.Companion.Zero

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

			BookMarkEvents.OnExportBookMarkPoints -> exportBookMarks()
		}
	}

	private fun onAddBookMark(bookMarkText: String?, time: LocalTime) {

		viewModelScope.launch {
			// bookmarks should be lesser than recorderTime
			val bookMarkTime = time.roundToClosestSeconds()
			val presentBookmarkTime = bookmarksProvider.getRecordingBookmarksFromIdAsList(audioId)
				.map { it.timeStamp }

			if (bookMarkTime in presentBookmarkTime) {
				return@launch
			}

			val result = if (bookMarkText != null) {
				bookmarksProvider.createBookMark(audioId, bookMarkTime, bookMarkText)
			} else {
				val collection = setOf(bookMarkTime)
				bookmarksProvider.createBookMarks(audioId, collection)
			}
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: "Cannot add bookmark"
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				is Resource.Success -> _createOrEditBookMarkState.update { CreateBookmarkState() }

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
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				is Resource.Success -> _createOrEditBookMarkState.update { CreateBookmarkState() }

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

	private fun exportBookMarks() = viewModelScope.launch {
		val bookMarks = bookmarksProvider.getRecordingBookmarksFromIdAsList(audioId).ifEmpty {
			_uiEvents.emit(UIEvents.ShowToast("No Bookmarks selected"))
			return@launch
		}

		val result = sharingUtil.shareBookmarksCsv(bookMarks)

		(result as? Resource.Error)?.let { res ->
			val message = res.message ?: res.error.message ?: "Cannot add bookmark"
			_uiEvents.emit(UIEvents.ShowToast(message))
		}
	}
}