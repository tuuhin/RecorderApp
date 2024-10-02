package com.eva.recorderapp.voice_recorder.domain.location.exceptions

class CannotFoundLastLocationException :
	Exception("Last location is not set, please use current location")