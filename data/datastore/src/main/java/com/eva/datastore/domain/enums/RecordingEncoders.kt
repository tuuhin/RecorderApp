package com.eva.datastore.domain.enums

enum class RecordingEncoders(val isCustom: Boolean = false) {
    // generic android encoders for recording
    ACC(false),
    OPTUS(false),
    AMR_NB(false),
    AMR_WB(false),

    // custom ones build with native libraries
    WAV(true),
    FLAC(true)
}
