package com.eva.interactions.data.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.eva.interactions.domain.enums.BtSCOChannelState

internal class BluetoothScoReceiver(
	private val onNewState: (BtSCOChannelState) -> Unit,
) : BroadcastReceiver() {

	override fun onReceive(context: Context?, intent: Intent?) {
		val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
			?: return

		val newState = when (state) {
			AudioManager.SCO_AUDIO_STATE_CONNECTED -> BtSCOChannelState.CONNECTED
			AudioManager.SCO_AUDIO_STATE_CONNECTING -> BtSCOChannelState.CONNECTING
			AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> BtSCOChannelState.DISCONNECTED
			AudioManager.SCO_AUDIO_STATE_ERROR -> BtSCOChannelState.ERROR
			else -> return
		}
		onNewState(newState)
	}
}
