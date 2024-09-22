package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PlayerFileProviderFromIdUseCase(
	private val playerFileProvider: PlayerFileProvider,
	private val metadataProvider: RecordingsSecondaryDataProvider,
) {
	operator fun invoke(audioId: Long): Flow<Resource<AudioFileModel, Exception>> {

		val fileProviderFlow = playerFileProvider.getAudioFileInfo(audioId)
		val metaDataFlow = metadataProvider.getRecordingFromIdAsFlow(audioId)

		return combine(fileProviderFlow, metaDataFlow) { resource, metadata ->
			when (resource) {
				is Resource.Success -> {
					val result = resource.data.copy(
						isFavourite = metadata?.isFavourite ?: false
					)
					Resource.Success(result)
				}

				else -> resource
			}
		}
	}
}