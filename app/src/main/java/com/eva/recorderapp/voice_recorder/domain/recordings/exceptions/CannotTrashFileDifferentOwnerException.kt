package com.eva.recorderapp.voice_recorder.domain.recordings.exceptions

class CannotTrashFileDifferentOwnerException :
	Exception("Cannot trash files with a different owner,those files aren't trashed")