package com.eva.recorderapp.voice_recorder.data.recordings.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.eva.recorderapp.voice_recorder.data.recordings.database.relations.CategoryRecordingsRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRecordingsDao {

	@Transaction
	@Query("SELECT * FROM recordings_category")
	fun getCategoryWithRecordingAsFlow(): Flow<List<CategoryRecordingsRelation>>

	@Transaction
	@Query("SELECT * FROM recordings_category")
	suspend fun getCategoryWithRecordingsAsList(): List<CategoryRecordingsRelation>

}