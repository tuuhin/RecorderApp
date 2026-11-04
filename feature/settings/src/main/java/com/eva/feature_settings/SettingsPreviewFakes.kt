package com.eva.feature_settings

import com.eva.feature_settings.utils.SpeechToTextUIModel
import com.eva.transcribe.domain.models.STTModel

internal object SettingsPreviewFakes {

    val STT_MODELS_LIST = listOf(
        STTModel(
            modelId = "Custom model 01",
            name = "English",
            displayName = "English",
        ),
        STTModel(
            modelId = "Custom model 02",
            name = "Hindi",
            displayName = "Hindi",

            ),
        STTModel(
            modelId = "Custom model 03",
            name = "bn",
            displayName = "Bangla",
        ),
    ).map { SpeechToTextUIModel(it) }

    val STT_MODEL = SpeechToTextUIModel(
        STTModel(
            modelId = "Custom model 01",
            name = "English",
            displayName = "English",
        ),
    )
}
