package com.eva.use_case.usecases

import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class RenameRecordingUseCase(private val recordingsProvider: VoiceRecordingsProvider) {

	operator fun invoke(recordingId: Long, newName: String): Flow<Resource<Unit, Exception>> {
		return flow {
			val trimmedName = newName.trim()

			if (trimmedName.isEmpty()) {
				emit(Resource.Error(Exception("New name cannot be empty")))
				return@flow
			}
			// emit the first loading
			emit(Resource.Loading)

			when (val res = recordingsProvider.getVoiceRecordingAsResourceFromId(recordingId)) {
				is Resource.Success -> {
					val updateFlow = recordingsProvider.renameRecording(
						recording = res.data,
						newName = trimmedName
					)
					emitAll(updateFlow)
				}

				is Resource.Error -> emit(Resource.Error(res.error, res.message))

				else -> {}
			}
		}
	}
}