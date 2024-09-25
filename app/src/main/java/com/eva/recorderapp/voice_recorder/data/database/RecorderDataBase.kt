package com.eva.recorderapp.voice_recorder.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eva.recorderapp.voice_recorder.data.database.convertors.CategoriesEnumConvertors
import com.eva.recorderapp.voice_recorder.data.database.convertors.LocalDateTimeConvertors
import com.eva.recorderapp.voice_recorder.data.database.convertors.LocalTimeConvertors
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.dao.TrashFileDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.data.database.entity.TrashFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
	entities = [
		TrashFileEntity::class,
		RecordingsMetaDataEntity::class,
		RecordingCategoryEntity::class,
		RecordingBookMarkEntity::class,
	],
	version = 5,
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
		AutoMigration(from = 3, to = 4),
		AutoMigration(from = 4, to = 5),
	]
)
@TypeConverters(
	value = [
		LocalDateTimeConvertors::class,
		LocalTimeConvertors::class,
		CategoriesEnumConvertors::class
	],
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFileDao

	abstract fun categoriesDao(): RecordingCategoryDao

	abstract fun recordingMetaData(): RecordingsMetadataDao

	abstract fun recordingBookMarkDao(): RecordingsBookmarkDao

	companion object {

		@Volatile
		private var instance: RecorderDataBase? = null

		private val localDateTimeConvertor = LocalDateTimeConvertors()
		private val localtimeConvertor = LocalTimeConvertors()
		private val categoryEnumConvertor = CategoriesEnumConvertors()

		fun createDataBase(context: Context): RecorderDataBase {
			return synchronized(this) {
				if (instance == null) {
					instance = Room.databaseBuilder(
						context,
						RecorderDataBase::class.java,
						DataBaseConstants.DATABASE_NAME
					)
						.addTypeConverter(localtimeConvertor)
						.addTypeConverter(localDateTimeConvertor)
						.addTypeConverter(categoryEnumConvertor)
						.setQueryExecutor(Dispatchers.IO.asExecutor())
						.build()
				}
				instance!!
			}
		}
	}
}