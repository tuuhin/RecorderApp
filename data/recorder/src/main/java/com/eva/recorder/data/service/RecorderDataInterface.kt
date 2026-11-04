package com.eva.recorder.data.service

import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

internal interface RecorderDataInterface {
    val recorderTime: Flow<LocalTime>
    val recorderState: StateFlow<RecorderState>
    val bookMarks: Flow<Set<Duration>>
    val amplitudes: Flow<List<RecordedPoint>>
    val transcript: Flow<String>
    val warnings: Flow<String>
}
