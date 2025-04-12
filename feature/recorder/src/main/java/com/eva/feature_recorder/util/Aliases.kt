package com.eva.feature_recorder.util

import kotlinx.datetime.LocalTime

internal typealias DeferredRecordingDataPointList = () -> List<Pair<LocalTime, Float>>
internal typealias DeferredLocalTimeList = () -> Iterable<LocalTime>