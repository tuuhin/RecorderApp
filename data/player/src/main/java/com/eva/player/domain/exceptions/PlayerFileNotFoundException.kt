package com.eva.player.domain.exceptions

class PlayerFileNotFoundException :
	Exception("Queried audio file Id is not found, please verify the source")