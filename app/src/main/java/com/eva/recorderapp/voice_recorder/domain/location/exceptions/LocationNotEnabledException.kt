package com.eva.recorderapp.voice_recorder.domain.location.exceptions

class LocationNotEnabledException :
	Exception("Location Not Enabled, need to turn on location to fetch the location info")