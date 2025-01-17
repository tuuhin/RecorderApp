package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalTime

typealias RecordingDataPointCallback = () -> List<Pair<LocalTime, Float>>
typealias PlayerGraphData = () -> List<Float>
typealias CategoryImmutableList = ImmutableList<RecordingCategoryModel>
typealias PlayRation = () -> Float
typealias BookMarksDeferredCallback = () -> ImmutableList<LocalTime>