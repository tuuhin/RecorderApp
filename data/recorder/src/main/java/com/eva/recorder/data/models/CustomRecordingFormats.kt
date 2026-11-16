package com.eva.recorder.data.models

import android.webkit.MimeTypeMap
import com.eva.datastore.domain.enums.RecordingEncoders

internal enum class CustomRecordingFormats {

    WAV,
    FLAC;

    val mimeType: String
        get() = when (this) {
            WAV -> mimeTypesMap.getExtensionFromMimeType("wav") ?: "audio/x-wav"
            FLAC -> mimeTypesMap.getExtensionFromMimeType("flac") ?: "audio/flac"
        }

    val extension: String?
        get() = mimeTypesMap.getExtensionFromMimeType(mimeType)
            ?.let { ext -> ".$ext" }

    companion object {
        private val mimeTypesMap = MimeTypeMap.getSingleton()

        fun fromEncoder(encoder: RecordingEncoders): CustomRecordingFormats {
            return when (encoder) {
                RecordingEncoders.FLAC -> FLAC
                RecordingEncoders.WAV -> WAV
                else -> throw IllegalStateException("Use android generic media")
            }
        }
    }
}
