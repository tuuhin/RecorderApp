package com.eva.recorderapp.voice_recorder.domain.location.exceptions

class CurrentLocationTimeoutException :
	Exception("Cannot determine the current location in the given time,result in a timeout")