package com.eva.location.domain.exceptions

class GeoCoderMissingException :
	Exception("Geo coder is not present in the device cannot decode location")