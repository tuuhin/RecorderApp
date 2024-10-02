package com.eva.recorderapp.voice_recorder.domain.location

fun interface LocationAddressProvider {

	suspend operator fun invoke(locationModel: BaseLocationModel): String?
}