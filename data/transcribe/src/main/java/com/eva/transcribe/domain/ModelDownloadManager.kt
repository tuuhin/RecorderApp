package com.eva.transcribe.domain

import com.eva.transcribe.domain.models.LanguageModel
import com.eva.transcribe.domain.models.ModelDownloadState
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ModelDownloadManager {

	suspend fun downloadAndSaveModel(language: LanguageModel): Flow<Resource<ModelDownloadState, Exception>>
}