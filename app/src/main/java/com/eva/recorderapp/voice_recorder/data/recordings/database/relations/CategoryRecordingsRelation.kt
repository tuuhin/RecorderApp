package com.eva.recorderapp.voice_recorder.data.recordings.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingsMetaDataEntity

data class CategoryRecordingsRelation(
	@Embedded
	val category: RecordingCategoryEntity,

	@Relation(
		parentColumn = "CATEGORY_ID",
		entityColumn = "CATEGORY_ID"
	)
	val recordings: List<RecordingsMetaDataEntity>
)