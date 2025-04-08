package com.eva.location.domain.exceptions

class LocationProviderNotFoundException :
	Exception("Cannot find any location provider to fetch location events")