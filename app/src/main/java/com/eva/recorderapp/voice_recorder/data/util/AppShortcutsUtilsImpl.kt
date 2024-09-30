package com.eva.recorderapp.voice_recorder.data.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.util.AppShortcutFacade
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks

private const val TAG = "APP_SHORTCUTS"

class AppShortcutsUtilsImpl(
	private val context: Context,
) : AppShortcutFacade {

	private val recordingShortcutId = "recording_shortcut"
	private val lastRecordingId = "last_recording_shortcut"

	private val recordingsShortCut: ShortcutInfoCompat
		get() = ShortcutInfoCompat.Builder(context, recordingShortcutId)
			.setRank(0)
			.setShortLabel(context.getString(R.string.app_shortcuts_recordings))
			.setLongLabel(context.getString(R.string.app_shortcuts_recordings_text))
			.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_recordings))
			.setIntent(
				Intent(context, MainActivity::class.java).apply {
					action = Intent.ACTION_VIEW
					data = NavDeepLinks.recordingsDestinationUri
				},
			)
			.build()

	private fun lastPlayedShortCut(audioId: Long): ShortcutInfoCompat {
		return ShortcutInfoCompat.Builder(context, lastRecordingId)
			.setRank(1)
			.setShortLabel(context.getString(R.string.app_shortcuts_open_last_played))
			.setLongLabel(context.getString(R.string.app_shortcuts_open_last_played_text))
			.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_play))
			.setIntent(
				Intent(context, MainActivity::class.java).apply {
					action = Intent.ACTION_VIEW
					data = NavDeepLinks.audioPlayerDestinationUri(audioId)
				},
			)
			.build()
	}


	override fun createRecordingsShortCut() {
		// remove the shortcut
		ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(recordingShortcutId))
		// then add the new one
		ShortcutManagerCompat.pushDynamicShortcut(context, recordingsShortCut)
		// this helps in dev process
		Log.d(TAG, "SHORTCUTS ADDED..")
	}

	override fun addLastPlayedShortcut(audioId: Long) {

		val lastPlayedShortcut = lastPlayedShortCut(audioId)

		val isSuccess = ShortcutManagerCompat.pushDynamicShortcut(context, lastPlayedShortcut)

		Log.d(TAG, "LAST PLAYED SHORTCUT ADDED : $isSuccess")
	}
}