package com.eva.transcribe.domain

import java.io.File

fun interface ModelFileProvider {

	suspend fun provideModelFile(language: LanguageModel): File?
}