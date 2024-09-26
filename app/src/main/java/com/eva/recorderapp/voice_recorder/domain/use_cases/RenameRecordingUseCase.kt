package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class RenameRecordingUseCase(
	private val recordingsProvider: VoiceRecordingsProvider,
) {
	operator fun invoke(recordingId: Long, newName: String): Flow<Resource<Boolean, Exception>> {
		val trimmedName = newName.trim()
		return flow {
			when (val res = recordingsProvider.getVoiceRecordingAsResourceFromId(recordingId)) {
				is Resource.Error -> emit(Resource.Error(res.error, res.message))

				is Resource.Success -> {
					val updateFlow = recordingsProvider.renameRecording(
						recording = res.data,
						newName = trimmedName
					)
					emitAll(updateFlow)
				}

				else -> emit(Resource.Loading)
			}
		}
	}
}