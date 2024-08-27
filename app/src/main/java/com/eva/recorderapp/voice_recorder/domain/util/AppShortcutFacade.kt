package com.eva.recorderapp.voice_recorder.domain.util

interface AppShortcutFacade {

	fun createRecordingsShortCut()

	fun addLastPlayedShortcut(audioId: Long)
}