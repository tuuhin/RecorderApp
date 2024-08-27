package com.eva.recorderapp.voice_recorder.data.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.util.AppShortcutFacade
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks

private const val TAG = "APP_SHORTCUTS"

class AppShortcutsUtilsImpl(
	private val context: Context
) : AppShortcutFacade {

	private val shortcutManager by lazy { context.getSystemService<ShortcutManager>() }

	private val RECORDING_SHORTCUT_ID = "recording_shortcut"
	private val LAST_RECORDING_ID = "last_recording_shortcut"


	private val recordingsShortCut: ShortcutInfoCompat
		get() = ShortcutInfoCompat.Builder(context, RECORDING_SHORTCUT_ID)
			.setRank(0)
			.setShortLabel(context.getString(R.string.app_shortcuts_recordings))
			.setLongLabel(context.getString(R.string.app_shortcuts_recordings_text))
			.setIcon(IconCompat.createWithResource(context, R.drawable.ic_recorder))
			.setIntent(Intent(Intent.ACTION_VIEW, NavDeepLinks.recordingsDestinationUri))
			.build()

	private fun lastPlayedShortCut(audioId: Long): ShortcutInfoCompat {
		return ShortcutInfoCompat.Builder(context, LAST_RECORDING_ID)
			.setRank(1)
			.setShortLabel(context.getString(R.string.app_shortcuts_open_last_played))
			.setLongLabel(context.getString(R.string.app_shortcuts_open_last_played_text))
			.setIcon(IconCompat.createWithResource(context, R.drawable.ic_play))
			.setIntent(Intent(Intent.ACTION_VIEW, NavDeepLinks.audioPlayerDestinationUri(audioId)))
			.build()
	}


	override fun createRecordingsShortCut() {
		val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
			.map(ShortcutInfoCompat::getId)

		if (!shortcuts.contains(recordingsShortCut.id)) {
			ShortcutManagerCompat.pushDynamicShortcut(context, recordingsShortCut)
			Log.d(TAG, "SHOTCUTS ADDED..")
		}
	}

	override fun addLastPlayedShortcut(audioId: Long) {

		val lastPlayedShortcut = lastPlayedShortCut(audioId)

		val isSuccess = ShortcutManagerCompat.pushDynamicShortcut(context, lastPlayedShortcut)

		Log.d(TAG, "LAST PLAYED SHORTCUT ADDED : $isSuccess")
	}
}