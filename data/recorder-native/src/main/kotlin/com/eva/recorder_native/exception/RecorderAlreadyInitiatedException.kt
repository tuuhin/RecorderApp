package com.eva.recorder_native.exception

class RecorderAlreadyInitiatedException :
	IllegalStateException("Recorder is already ready release it to configure it again")