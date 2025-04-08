package com.eva.interactions.domain

interface AppShortcutFacade {

	fun createRecordingsShortCut()

	fun addLastPlayedShortcut(audioId: Long)
}