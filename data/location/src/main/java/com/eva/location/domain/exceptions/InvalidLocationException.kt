package com.eva.location.domain.exceptions

class InvalidLocationException : Exception("Provided location to geocoder is wrong, not in range")