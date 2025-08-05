package com.eva.recorder.data.service

import android.content.Context
import android.widget.Toast
import com.eva.recorder.R

internal fun Context.phoneRingingToast() =
	Toast.makeText(this, R.string.toast_incoming_call, Toast.LENGTH_SHORT)
		.show()

internal fun Context.showScoConnectToast() =
	Toast.makeText(this, R.string.toast_recording_bluetooth, Toast.LENGTH_LONG)
		.show()

internal fun Context.showBookmarksSavedMessage() =
	Toast.makeText(this, R.string.bookmarks_saved, Toast.LENGTH_LONG)
		.show()

internal fun Context.showSaveRecordingErrorMessage(message: String) =
	Toast.makeText(this, message, Toast.LENGTH_LONG)
		.show()