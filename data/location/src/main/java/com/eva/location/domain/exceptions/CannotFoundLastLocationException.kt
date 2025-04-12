package com.eva.location.domain.exceptions

class CannotFoundLastLocationException :
	Exception("Last location is not set, please use current location")