package com.eva.location.domain.exceptions

class CurrentLocationTimeoutException :
	Exception("Cannot determine the current location in the given time,result in a timeout")