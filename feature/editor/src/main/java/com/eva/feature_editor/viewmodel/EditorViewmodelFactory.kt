package com.eva.feature_editor.viewmodel

import com.eva.recordings.domain.models.AudioFileModel
import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface EditorViewmodelFactory {

	fun create(fileModel: AudioFileModel): AudioEditorViewModel
}