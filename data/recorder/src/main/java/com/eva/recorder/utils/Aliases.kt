package com.eva.recorder.utils

import com.eva.recorder.domain.models.RecordedPoint
import kotlin.time.Duration

typealias DeferredRecordedPointList = () -> List<RecordedPoint>
typealias DeferredDurationList = () -> Iterable<Duration>