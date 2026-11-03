package com.eva.feature_settings.screen

import androidx.lifecycle.viewModelScope
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.feature_settings.utils.SpeechToTextUIModel
import com.eva.feature_settings.utils.TranscriptionSettingsEvents
import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState
import com.eva.transcribe.domain.repository.STTModelsRepository
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import com.eva.worker.controller.STTModelDeleteController
import com.eva.worker.controller.STTModelDownloadController
import com.eva.worker.domain.WorkResource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class STTModelsManagerViewModel @Inject constructor(
    private val repository: STTModelsRepository,
    private val sttModelDownloader: STTModelDownloadController,
    private val sttModelDeleteController: STTModelDeleteController,
    private val settingsRepo: TranscriptionSettingsRepo,
) : AppViewModel() {

    private val _modelsInfo = MutableStateFlow<List<SpeechToTextUIModel>>(emptyList())
    val modelsInfo = _modelsInfo
        .onStart { onLoadAndObserveModels() }
        .map { models -> models.toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(8_000L),
            initialValue = persistentListOf(),
        )

    val transcriptionSettings = settingsRepo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TranscriptionSettings(),
        )

    override val uiEvent: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()


    fun onEvent(event: TranscriptionSettingsEvents) {
        when (event) {
            is TranscriptionSettingsEvents.OnDeleteDownloadedModel -> onDeleteModel(event.model.modelId)
            is TranscriptionSettingsEvents.OnStartModelDownload -> onStartDownload(event.model.modelId)
            is TranscriptionSettingsEvents.OnSelectModel -> onSelectModel(event.model)
            TranscriptionSettingsEvents.OnToggleTranscriptions -> toggleEnable()
            is TranscriptionSettingsEvents.OnSelectInvalidModel -> onSelectInvalidModel(event.model)
        }
    }

    private fun toggleEnable() = viewModelScope.launch {
        val isEnabled = transcriptionSettings.value.isEnabled
        settingsRepo.onEnableOrDisable(!isEnabled)
    }

    private fun onSelectModel(model: STTModel?) = viewModelScope.launch {
        if (model?.state == STTModelState.DOWNLOADED) settingsRepo.onUpdateModelId(model.modelId)
        else settingsRepo.onUpdateModelId(null)
    }

    private fun onSelectInvalidModel(model: STTModel) = viewModelScope.launch {
        val event = when (model.state) {
            STTModelState.UN_AVAILABLE -> UIEvents.ShowSnackBarWithActions(
                message = "Download required",
                action = { onStartDownload(model.modelId) },
                actionText = "Download",
            )

            STTModelState.DOWNLOADING -> UIEvents.ShowToast("Downloading model")
            else -> return@launch
        }
        uiEvent.emit(event)
    }

    private fun onDeleteModel(modelId: String) = viewModelScope.launch {
        val workerId = sttModelDeleteController.startWorker(modelId)
        sttModelDeleteController.observeWorker(workerId)
            .onEach { res ->
                val event = when (res) {
                    is WorkResource.Blocked -> UIEvents.ShowToast("Cannot delete the model now")
                    is WorkResource.Canceled if (res.reason.isNotBlank()) -> UIEvents.ShowSnackBar(res.reason)
                    is WorkResource.Enqueued -> UIEvents.ShowToast("Deleting model files")
                    is WorkResource.Failed if (res.reason.isNotBlank()) -> UIEvents.ShowSnackBar(res.reason)
                    is WorkResource.Running if (res.message.isNotBlank()) -> UIEvents.ShowSnackBar(res.message)
                    is WorkResource.Success if (!res.message.isNullOrBlank()) ->
                        UIEvents.ShowSnackBar(res.message ?: "Success")

                    else -> return@onEach
                }
                uiEvent.emit(event)
            }
            .launchIn(this)
    }

    private fun onStartDownload(modelId: String) = viewModelScope.launch {
        val workerId = sttModelDownloader.startWorker(modelId)
        sttModelDownloader.observeWorker(workerId)
            .onEach { res ->
                val event = when (res) {
                    is WorkResource.Blocked -> UIEvents.ShowToast("Cannot download the model now")
                    is WorkResource.Enqueued -> UIEvents.ShowSnackBar("Starting your download shortly")
                    is WorkResource.Canceled if (res.reason.isNotBlank()) -> UIEvents.ShowSnackBar(res.reason)
                    is WorkResource.Failed if (res.reason.isNotBlank()) -> UIEvents.ShowSnackBar(res.reason)
                    is WorkResource.Success if (res.message?.isNotBlank() == true) ->
                        UIEvents.ShowSnackBar(res.message ?: "")

                    else -> return@onEach
                }
                uiEvent.emit(event)
            }
            .launchIn(this)
    }


    private fun onLoadAndObserveModels() {
        val sttReads = repository.readAllSSTModels()

        val workRead = sttModelDownloader.observeDownloadWorkers()
            .onStart { emptyList<WorkResource>() }

        combine(sttReads, workRead) { resModels, workers -> resModels to workers }
            .onEach { (resModels, workers) ->

                when (resModels) {
                    is Resource.Error -> {
                        val message = resModels.message ?: resModels.error.message
                        ?: "Unable to load transcription models"
                        uiEvent.emit(UIEvents.ShowSnackBar(message))
                    }

                    is Resource.Success -> _modelsInfo.update {
                        resModels.data.map { model ->
                            // read in worker info progress and state
                            val workerResource = workers.findLast { it.modelId == model.modelId }
                            val progress = if (workerResource is WorkResource.Running)
                                workerResource.progress?.div(100f) else 0f

                            val isRunning = workerResource is WorkResource.Running
                            val workId = workerResource?.workId

                            SpeechToTextUIModel(
                                model = model,
                                progress = { progress ?: 0f },
                                isRunning = isRunning,
                                downloadId = workId,
                            )
                        }
                    }

                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }
}
