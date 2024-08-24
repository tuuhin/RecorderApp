package com.eva.recorderapp.voice_recorder.presentation.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks

class AppShortCuts(
	private val context: Context
) {

	val shortcutManager by lazy { context.getSystemService<ShortcutManager>() }

	private val RECORDING_SHORTCUT_ID = "recording_shortcut"
	private val RECORDER_SHORTCUT_ID = "recorder_shortcut"

	private val recordingsShortCut: ShortcutInfoCompat
		get() = ShortcutInfoCompat.Builder(context, RECORDING_SHORTCUT_ID)
			.setShortLabel(context.getString(R.string.recording_top_bar_title))
			.setLongLabel("Open app recordings")
			.setIcon(IconCompat.createWithResource(context, R.drawable.ic_recorder))
			.setIntent(Intent(Intent.ACTION_VIEW, NavDeepLinks.recordingsDestinationUri))
			.build()

	private val recorderShortcut: ShortcutInfoCompat
		get() = ShortcutInfoCompat.Builder(context, RECORDER_SHORTCUT_ID)
			.setShortLabel(context.getString(R.string.recorder_action_start))
			.setLongLabel("Start recordings")
			.setIcon(IconCompat.createWithResource(context, R.drawable.ic_record_player))
			.setIntent(Intent(Intent.ACTION_VIEW, NavDeepLinks.recorderDestinationUri))
			.build()

	fun attachShortcutsIfNotPresent() {
		// TODO: Add or remove shortcuts later if needed
		val shortcuts = ShortcutManagerCompat
			.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)

		if (!shortcuts.contains(recordingsShortCut)) {
			ShortcutManagerCompat.pushDynamicShortcut(context, recordingsShortCut)
		}
		if (!shortcuts.contains(recorderShortcut)) {
			ShortcutManagerCompat.pushDynamicShortcut(context, recorderShortcut)
		}
	}
}


