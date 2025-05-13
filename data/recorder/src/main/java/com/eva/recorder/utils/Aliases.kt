package com.eva.recorder.utils

import kotlin.time.Duration

typealias DurationToAmplitudeList = List<Pair<Duration, Float>>
typealias DeferredRecordingDataPointList = () -> List<Pair<Duration, Float>>
typealias DeferredDurationList = () -> Iterable<Duration>