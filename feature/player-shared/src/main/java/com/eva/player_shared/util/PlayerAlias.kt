package com.eva.player_shared.util

import com.eva.player_shared.state.ContentLoadState
import com.eva.recordings.domain.models.AudioFileModel

typealias PlayerGraphData = () -> List<Float>
typealias PlayRatio = () -> Float
typealias AudioFileModelLoadState = ContentLoadState<AudioFileModel>