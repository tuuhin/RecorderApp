package com.eva.player.domain.exceptions

class DecoderExistsException : Exception("Decoder is holding resources, clean it to run again")