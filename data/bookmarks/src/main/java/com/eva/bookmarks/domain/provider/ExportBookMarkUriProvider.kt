package com.eva.bookmarks.domain.provider

import com.eva.bookmarks.domain.AudioBookmarkModel

fun interface ExportBookMarkUriProvider {

	suspend operator fun invoke(points: Collection<AudioBookmarkModel>): String?
}