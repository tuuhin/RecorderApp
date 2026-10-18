package com.eva.database

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.eva.database.convertors.LocalDateTimeConvertors
import com.eva.database.convertors.LocalTimeConvertors
import com.eva.database.convertors.STTModelConvertors
import com.eva.database.dao.RecordingCategoryDao
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.dao.STTModelsDao
import com.eva.database.dao.TrashFileDao
import com.eva.database.entity.RecordingBookMarkEntity
import com.eva.database.entity.RecordingCategoryEntity
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.STTModelEntity
import com.eva.database.entity.TrashFileEntity
import kotlinx.coroutines.Dispatchers

@Database(
	entities = [
		TrashFileEntity::class,
		RecordingsMetaDataEntity::class,
		RecordingCategoryEntity::class,
		RecordingBookMarkEntity::class,
		STTModelEntity::class,
	],
	version = 7,
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
		AutoMigration(from = 3, to = 4),
		AutoMigration(from = 4, to = 5),
		AutoMigration(from = 6, to = 7)
	]
)
@ColumnTypeConverters(
	value = [
		LocalDateTimeConvertors::class,
		LocalTimeConvertors::class,
		STTModelConvertors::class
	],
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFileDao

	abstract fun categoriesDao(): RecordingCategoryDao

	abstract fun recordingMetaData(): RecordingsMetadataDao

	abstract fun recordingBookMarkDao(): RecordingsBookmarkDao

	abstract fun sTTModelDao(): STTModelsDao

	companion object {

		@Volatile
		private var instance: RecorderDataBase? = null

		private val localDateTimeConvertor = LocalDateTimeConvertors()
		private val localtimeConvertor = LocalTimeConvertors()
		private val sttStausConvertors = STTModelConvertors()

		fun createDataBase(context: Context): RecorderDataBase {
			return synchronized(this) {
				instance ?: Room.databaseBuilder(
					context,
					RecorderDataBase::class.java,
					DataBaseConstants.DATABASE_NAME
				)
					.addColumnTypeConverter(localtimeConvertor)
					.addColumnTypeConverter(localDateTimeConvertor)
					.addColumnTypeConverter(sttStausConvertors)
					.addMigrations(DBMigrations.MIGRATE_5_6)
					.setQueryCoroutineContext(Dispatchers.IO)
					.build()
					.also { db -> instance = db }
			}
		}

		fun createInMemoryDatabase(context: Context): RecorderDataBase {
			return Room.inMemoryDatabaseBuilder(context, RecorderDataBase::class.java)
				.addColumnTypeConverter(localtimeConvertor)
				.addColumnTypeConverter(localDateTimeConvertor)
				.addColumnTypeConverter(sttStausConvertors)
				.addMigrations(DBMigrations.MIGRATE_5_6)
				.setQueryCoroutineContext(Dispatchers.IO)
				.build()
		}
	}
}