package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction

interface RecorderActionHandler {

	fun onRecorderAction(action: RecorderAction): Resource<Unit, Exception>
}