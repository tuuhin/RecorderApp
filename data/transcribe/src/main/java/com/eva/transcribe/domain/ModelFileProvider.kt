package com.eva.transcribe.domain

import com.eva.transcribe.domain.models.LanguageModel
import java.io.File

interface ModelFileProvider {

	suspend fun provideModelFile(language: LanguageModel): File?

	suspend fun deleteModelInfo(languageModel: LanguageModel)
}