package com.eva.feature_player.bookmarks

import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface BookmarksViewmodelFactory {

	fun create(fileId: Long): BookMarksViewModel
}