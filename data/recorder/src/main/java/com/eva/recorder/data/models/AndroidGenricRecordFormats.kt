package com.eva.recorder.data.models

import android.media.MediaRecorder
import android.webkit.MimeTypeMap
import androidx.media3.common.MimeTypes
import com.eva.datastore.domain.enums.RecordingEncoders

internal enum class AndroidGenricRecordFormats(
    val encoder: Int,
    val outputFormat: Int,
    val mimeType: String,
) {

    THREE_GPP(
        encoder = MediaRecorder.AudioEncoder.AMR_NB,
        outputFormat = MediaRecorder.OutputFormat.AMR_NB,
        mimeType = MimeTypes.AUDIO_AMR,
    ),

    AMR_WB(
        encoder = MediaRecorder.AudioEncoder.AMR_WB,
        outputFormat = MediaRecorder.OutputFormat.AMR_WB,
        mimeType = MimeTypes.AUDIO_AMR_WB,
    ),

    M4A(
        encoder = MediaRecorder.AudioEncoder.AAC,
        outputFormat = MediaRecorder.OutputFormat.MPEG_4,
        mimeType = MimeTypes.AUDIO_MP4,
    ),

    OGG(
        encoder = MediaRecorder.AudioEncoder.OPUS,
        outputFormat = MediaRecorder.OutputFormat.OGG,
        mimeType = MimeTypes.AUDIO_OGG,
    );

    val extension: String?
        get() = mimeTypesMap.getExtensionFromMimeType(mimeType)
            ?.let { ext -> ".$ext" }

    companion object {

        private val mimeTypesMap = MimeTypeMap.getSingleton()

        fun fromEncoder(encoder: RecordingEncoders): AndroidGenricRecordFormats {
            return when (encoder) {
                RecordingEncoders.AMR_NB -> THREE_GPP
                RecordingEncoders.AMR_WB -> AMR_WB
                RecordingEncoders.ACC -> M4A
                RecordingEncoders.OPTUS -> OGG
                else -> throw IllegalStateException("Use custom media recorder")
            }
        }
    }
}


