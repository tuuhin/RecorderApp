package com.eva.location.domain.repository

import com.eva.location.domain.BaseLocationModel

interface LocationProvider {

	val isLocationEnabled: Boolean

	suspend operator fun invoke(fetchCurrentIfNotFound: Boolean = true): Result<BaseLocationModel>
}