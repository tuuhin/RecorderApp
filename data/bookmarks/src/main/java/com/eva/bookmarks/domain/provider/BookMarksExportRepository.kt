package com.eva.bookmarks.domain.provider

import com.eva.bookmarks.domain.AudioBookmarkModel

interface BookMarksExportRepository {

	suspend fun invoke(points: List<AudioBookmarkModel>): String?

}