package com.eva.transcribe.domain

import java.io.File

interface ModelFileProvider {

	suspend fun provideModelFile(language: LanguageModel): File?

	suspend fun deleteModelInfo(languageModel: LanguageModel)
}