package com.eva.recordings.domain.provider

import java.io.File

fun interface AudioTagWriter {

	suspend fun writeAuthorTag(file: File, artistName: String?): Boolean
}