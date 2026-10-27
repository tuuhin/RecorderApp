package com.eva.recorder_native

class FlacAudioRecorder {


	companion object {
		init {
			System.loadLibrary("custom_flac")
		}
	}
}