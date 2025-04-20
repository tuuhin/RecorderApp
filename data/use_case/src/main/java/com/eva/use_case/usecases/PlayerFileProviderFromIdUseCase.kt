package com.eva.use_case.usecases

import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class PlayerFileProviderFromIdUseCase(
	private val playerFileProvider: PlayerFileProvider,
	private val metadataProvider: RecordingsSecondaryDataProvider,
) {
	operator fun invoke(audioId: Long): Flow<Resource<AudioFileModel, Exception>> {
		val fileDataFlow = playerFileProvider.getAudioFileFromIdFlow(audioId)
		val metaDataflow = metadataProvider.getRecordingFromIdAsFlow(audioId)
			.catch { err -> err.printStackTrace() }

		return combine(
			flow = fileDataFlow,
			flow2 = metaDataflow
		) { fileData, metadata ->
			when (fileData) {
				is Resource.Success<AudioFileModel, Exception> -> {
					val result = fileData.data.copy(
						isFavourite = metadata?.isFavourite == true
					)
					Resource.Success(result)
				}

				else -> fileData
			}
		}
	}
}