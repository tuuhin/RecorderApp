package com.eva.recorderapp.voice_recorder.domain.location

import com.eva.recorderapp.common.Resource

fun interface LocationProvider {

	suspend operator fun invoke(): Resource<BaseLocationModel, Exception>
}