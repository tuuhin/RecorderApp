package com.eva.recorderapp.voice_recorder.presentation.recordings.search

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchRecordingsViewmodel @Inject constructor(
	private val categoriesProvider: RecordingCategoryProvider,
) : AppViewModel() {

	private val _categories = MutableStateFlow(emptyList<RecordingCategoryModel>())
	private val _recordings = MutableStateFlow(emptyList<RecordedVoiceModel>())

	private val _categoryFilter = MutableStateFlow<RecordingCategoryModel?>(null)
	private val _timeFilter = MutableStateFlow<SearchFilterTimeOption?>(null)
	private val _searchQuery = MutableStateFlow("")

	@OptIn(FlowPreview::class)
	private val _debouncedSearchQuery: Flow<String>
		get() = _searchQuery.debounce(600.milliseconds)

	val searchState = combine(
		_searchQuery,
		_categoryFilter,
		_timeFilter,
		transform = ::SearchRecordingScreenState
	).stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = SearchRecordingScreenState()
	)

	val recordings = combine(
		_recordings, _categoryFilter, _timeFilter, _debouncedSearchQuery
	) { recordings, category, time, query ->

		val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
		val yesterday = today.minus(DatePeriod(days = 1))
		val lastMonth = today.minus(DatePeriod(months = 1))

		recordings.filter { model ->
			val recordedAt = model.recordedAt.date

			val timeFilter = when (time) {
				SearchFilterTimeOption.TODAY -> recordedAt == today
				SearchFilterTimeOption.YESTERDAY -> recordedAt == yesterday
				SearchFilterTimeOption.WEEK -> with(recordedAt) {
					val startOfWeek = today.minus(DatePeriod(days = dayOfWeek.value))
					val endOfWeek = today.plus(DatePeriod(days = 7 - dayOfWeek.value))
					recordedAt in startOfWeek..endOfWeek
				}

				SearchFilterTimeOption.THIS_MONTH -> recordedAt.month == today.month
				SearchFilterTimeOption.LAST_MONTH -> recordedAt.month == lastMonth.month
				SearchFilterTimeOption.THIS_YEAR -> recordedAt.year == today.year
				null -> true
			}

			val isCategorySame = category?.id?.let { it == model.categoryId } ?: true

			isCategorySame && timeFilter && model.displayName.contains(query)
		}.toImmutableList()

	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = persistentListOf()
	)

	val categories = _categories
		.map { categories -> categories.toImmutableList() }
		.onStart { populateRecordingCategories() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()

	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onEvent(event: SearchRecordingScreenEvent) {
		when (event) {
			is SearchRecordingScreenEvent.OnCategorySelected -> _categoryFilter.update { event.category }
			is SearchRecordingScreenEvent.OnSelectTimeFilter -> _timeFilter.update { event.filter }
			is SearchRecordingScreenEvent.OnQueryChange -> _searchQuery.update { event.text }
		}
	}

	private fun populateRecordingCategories() {
		categoriesProvider.recordingCategoryAsResourceFlow
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "SOME ERROR"
						_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
					}

					is Resource.Success -> _categories.update { res.data }
					else -> {}
				}
			}.launchIn(viewModelScope)
	}

	private fun populateRecordings() {

	}


}