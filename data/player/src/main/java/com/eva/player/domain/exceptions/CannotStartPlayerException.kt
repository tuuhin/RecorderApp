package com.eva.player.domain.exceptions

class CannotStartPlayerException :
	Exception("Cannot configure as some other thread maybe preparing the player")