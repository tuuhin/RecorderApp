package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction

interface RecorderActionHandler {

	fun onRecorderAction(action: RecorderAction)
}