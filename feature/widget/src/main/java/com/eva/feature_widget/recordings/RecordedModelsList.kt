package com.eva.feature_widget.recordings

import androidx.compose.runtime.Immutable
import com.eva.recordings.domain.models.RecordedVoiceModel

@Immutable
internal data class RecordedModelsList(val recordings: List<RecordedVoiceModel> = emptyList())
