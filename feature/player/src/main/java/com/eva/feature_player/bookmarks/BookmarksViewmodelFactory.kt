package com.eva.feature_player.bookmarks

import com.eva.recordings.domain.models.AudioFileModel
import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface BookmarksViewmodelFactory {

	fun create(fileModel: AudioFileModel): BookMarksViewModel
}