package com.eva.recordings.domain.exceptions

class CannotTrashFileDifferentOwnerException :
	Exception("Cannot trash files with a different owner,those files aren't trashed")