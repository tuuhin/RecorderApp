package com.eva.recorderapp.voice_recorder.domain.location.exceptions

class LocationProviderNotFoundException :
	Exception("Cannot find any location provider to fetch location events")