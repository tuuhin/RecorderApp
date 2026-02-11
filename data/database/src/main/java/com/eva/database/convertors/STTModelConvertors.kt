package com.eva.database.convertors

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.eva.database.entity.STTModelEntity

@ProvidedTypeConverter
class STTModelConvertors {

	@TypeConverter
	fun fromModelStatus(state: Int): STTModelEntity.ModelStatus {
		return STTModelEntity.ModelStatus.entries.find { it.flag == state }
			?: STTModelEntity.ModelStatus.NOT_READY
	}

	@TypeConverter
	fun toModelStatus(value: STTModelEntity.ModelStatus): Int = value.flag
}