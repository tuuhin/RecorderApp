package com.eva.recorder.domain

import com.eva.recorder.domain.models.RecorderAction
import com.eva.utils.Resource

fun interface RecorderActionHandler {

	fun onRecorderAction(action: RecorderAction): Resource<Unit, Exception>
}