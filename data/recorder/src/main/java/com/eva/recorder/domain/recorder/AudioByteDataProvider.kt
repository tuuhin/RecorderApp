package com.eva.recorder.domain.recorder

import kotlinx.coroutines.flow.Flow

internal interface AudioByteDataProvider {

	val stream: Flow<Pair<ShortArray, Int>>
}