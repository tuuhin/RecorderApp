package com.eva.feature_player.bookmarks.utils

import com.eva.bookmarks.domain.AudioBookmarkModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.toKotlinLocalTime
import java.time.LocalTime

internal object BookmarksPreviewFakes {

	val FAKE_BOOKMARK_MODEL = AudioBookmarkModel(
		bookMarkId = 0L,
		text = "Android",
		timeStamp = LocalTime.now().toKotlinLocalTime(),
		recordingId = 0L
	)

	val FAKE_BOOKMARKS_LIST = List(20) {
		AudioBookmarkModel(
			bookMarkId = it.toLong(),
			text = "Hello world",
			timeStamp = kotlinx.datetime.LocalTime.Companion.fromSecondOfDay(400),
			recordingId = 0L
		)
	}.toPersistentList()

}