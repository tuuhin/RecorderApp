package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalTime

typealias RecordingDataPointCallback = () -> List<Pair<LocalTime, Float>>
typealias PlayerGraphData = () -> List<Float>
typealias CategoryImmutableList = ImmutableList<RecordingCategoryModel>
typealias RecordedVoiceModelsList = ImmutableList<RecordedVoiceModel>
typealias PlayRation = () -> Float
typealias BookMarksDeferredCallback = () -> ImmutableList<LocalTime>