package com.eva.recorder.domain.recorder

import com.eva.recorder.domain.models.RecordedPoint
import kotlinx.coroutines.flow.Flow

interface AudioVisualDataProvider {

	val dataPoints: Flow<List<RecordedPoint>>
}