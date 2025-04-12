package com.eva.location.domain.repository

import com.eva.location.domain.BaseLocationModel

fun interface LocationAddressProvider {

	suspend operator fun invoke(locationModel: BaseLocationModel): String?
}