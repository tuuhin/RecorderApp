package com.eva.location.domain.repository

import com.eva.location.domain.BaseLocationModel
import com.eva.utils.Resource

fun interface LocationProvider {

	suspend operator fun invoke(): Resource<BaseLocationModel, Exception>
}