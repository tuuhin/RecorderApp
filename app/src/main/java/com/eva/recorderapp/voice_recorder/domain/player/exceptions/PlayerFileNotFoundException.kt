package com.eva.recorderapp.voice_recorder.domain.player.exceptions

class PlayerFileNotFoundException :
	Exception("Queried audio file Id is not found, please verify the source")