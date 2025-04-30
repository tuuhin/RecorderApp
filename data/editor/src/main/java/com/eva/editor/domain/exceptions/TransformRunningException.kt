package com.eva.editor.domain.exceptions

class TransformRunningException :
	Exception("Some transformation is going on, try again after it completes")