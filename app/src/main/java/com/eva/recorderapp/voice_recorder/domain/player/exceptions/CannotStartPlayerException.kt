package com.eva.recorderapp.voice_recorder.domain.player.exceptions

class CannotStartPlayerException :
	Exception("Cannot configure as some other thread maybe preparing the player")