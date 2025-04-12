package com.eva.interactions.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.eva.interactions.R
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.utils.IntentConstants
import com.eva.utils.NavDeepLinks

private const val TAG = "APP_SHORTCUTS"

internal class AppShortcutsUtilsImpl(private val context: Context) : AppShortcutFacade {

	private val recordingShortcutId = "recording_shortcut"
	private val lastRecordingShortCutId = "last_recording_shortcut"


	private val recordingsShortCut: ShortcutInfoCompat
		get() = ShortcutInfoCompat.Builder(context, recordingShortcutId)
			.setRank(0)
			.setShortLabel(context.getString(R.string.app_shortcuts_recordings))
			.setLongLabel(context.getString(R.string.app_shortcuts_recordings_text))
			.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_recordings))
			.setIntent(
				Intent().apply {
					setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
					action = Intent.ACTION_VIEW
					data = NavDeepLinks.RECORDING_DESTINATION_PATTERN.toUri()
				},
			)
			.build()

	private fun lastPlayedShortCut(audioId: Long): ShortcutInfoCompat {
		return ShortcutInfoCompat.Builder(context, lastRecordingShortCutId)
			.setRank(1)
			.setShortLabel(context.getString(R.string.app_shortcuts_open_last_played))
			.setLongLabel(context.getString(R.string.app_shortcuts_open_last_played_text))
			.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_play))
			.setIntent(
				Intent().apply {
					setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
					action = Intent.ACTION_VIEW
					data = NavDeepLinks.audioPlayerDestinationUri(audioId).toUri()
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