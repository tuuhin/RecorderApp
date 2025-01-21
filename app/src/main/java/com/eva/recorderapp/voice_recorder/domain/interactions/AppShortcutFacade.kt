package com.eva.recorderapp.voice_recorder.domain.interactions

interface AppShortcutFacade {

	fun createRecordingsShortCut()

	fun addLastPlayedShortcut(audioId: Long)
}