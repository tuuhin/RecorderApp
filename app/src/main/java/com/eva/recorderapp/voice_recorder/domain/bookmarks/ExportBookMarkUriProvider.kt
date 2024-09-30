package com.eva.recorderapp.voice_recorder.domain.bookmarks

fun interface ExportBookMarkUriProvider {

	suspend operator fun invoke(points: Collection<AudioBookmarkModel>): String?
}