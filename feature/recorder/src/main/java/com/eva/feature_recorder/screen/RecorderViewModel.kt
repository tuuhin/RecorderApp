package com.eva.feature_recorder.screen

import androidx.lifecycle.viewModelScope
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.feature_recorder.util.TranslationDataBlock
import com.eva.recorder.domain.RecorderActionHandler
import com.eva.recorder.domain.RecorderServiceBinder
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
internal class RecorderViewModel @Inject constructor(
	private val handler: RecorderActionHandler,
	private val recorderService: RecorderServiceBinder,
	transcriptionSettings: TranscriptionSettingsRepo,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val isServiceReady: StateFlow<Boolean> = recorderService.isConnectionReady

	val recorderState = recorderService.recorderState
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = RecorderState.IDLE
		)

	val recorderTime = recorderService.recorderTimer
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = LocalTime(0, 0)
		)

	val bookMarksSet = recorderService.bookMarkTimes
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = emptySet()
		)

	val recordingPoints = recorderService.amplitudes
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = emptyList()
		)

	@OptIn(ExperimentalCoroutinesApi::class)
	val transcriptionsResult = recorderService.recorderState
		.flatMapLatest { state ->
			if (!state.canReadAmplitudes) flowOf(TranslationDataBlock())
			else controlledTranscriptions(silenceThreshold = 2.seconds)
		}
		.flowOn(Dispatchers.Default)
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = TranslationDataBlock(),
		)

	val transcribeSettings = transcriptionSettings.settingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000L),
			initialValue = TranscriptionSettings()
		)

	fun onAction(action: RecorderAction) {

		when (val resource = handler.onRecorderAction(action)) {
			is Resource.Error -> viewModelScope.launch {
				val message = resource.error.message ?: resource.message ?: ""
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}


	fun onEvent(event: RecorderScreenEvent) {
		when (event) {
			RecorderScreenEvent.BindRecorderService -> recorderService.bindToService()
			RecorderScreenEvent.UnBindRecorderService -> recorderService.unBindService()
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
	private fun controlledTranscriptions(
		maxWords: Int = 6,
		silenceThreshold: Duration = 1.seconds
	): Flow<TranslationDataBlock> {

		var blockCounter = 0
		val space = "\\s+".toRegex()
		val previousBlock = MutableStateFlow<List<String>>(emptyList())

		return recorderService.transcriptions
			.filter { it.isNotBlank() }
			.transformLatest { newText ->
				val words = newText.split(space)
					.filter(String::isNotBlank)

				// check if elements have any match
				if (!previousBlock.value.any { it in words })
					blockCounter++

				val result = words.takeLast(maxWords).joinToString(" ")

				if (result.isNotBlank()) {
					val activeBlock = TranslationDataBlock(blockCounter, result)
					emit(activeBlock)
					// set the new block of words
					previousBlock.value = words
				}

				// a delay to skip some
				delay(silenceThreshold)
				blockCounter++
				emit(TranslationDataBlock(blockNumber = blockCounter))
			}
	}


	override fun onCleared() {
		recorderService.unBindService()
		recorderService.cleanUp()
	}
}