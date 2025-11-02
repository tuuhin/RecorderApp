package com.eva.player.domain

import com.eva.recordings.domain.models.AudioFileModel
import kotlin.time.Duration

interface AudioMetadataRetriever {

	suspend fun retrieveAudioDuration(model: AudioFileModel): Duration?
}