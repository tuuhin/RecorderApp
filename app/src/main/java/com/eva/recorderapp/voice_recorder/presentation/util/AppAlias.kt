package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.presentation.categories.utils.SelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalTime

typealias RecordingDataPointCallback = () -> List<Pair<LocalTime,Float>>
typealias PlayerGraphData = () -> PlayerGraphInfo
typealias SelectableCategoryImmutableList = ImmutableList<SelectableCategory>