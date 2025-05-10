package com.eva.worker

object WorkerParams {

	// remove trash recording worker params
	const val REMOVE_TRASH_RECORDING_SUCCESS_KEY = "remove_trash_recording_api_29_success"
	const val REMOVE_TRASH_RECORDING_FAILED_KEY = "remove_trash_recordings_api_29_failed"

	// update path worker
	const val UPDATE_RECORDING_PATH_SUCCESS_KEY = "update_path_success"
	const val UPDATE_RECORDING_PATH_FAILED_KEY = "update_path_failed_key"


	// save edit media worker
	const val WORK_DATA_FILE_URI = "WORK_DATA_EDIT_ITEM_FILE_URI"
	const val WORK_DATA_FILE_NAME = "WORK_FILE_NAME"
	const val WORK_DATA_FILE_MIME_TYPE = "WORK FILE_MIME_TYPE"

	const val WORK_DATA_REQUIRED_ITEMS_NOT_FOUND = "WORK_DATA REQUIREMENTS NOT FOUND"
	const val WORK_SAVE_EDITED_ITEM_FAILED = "SAVE EDIT ITEM CONTENT FAILED"

	const val WORK_DATA_SAVE_EDITED_ITEM_FILE_NAME_NOT_PROVIDED =
		"FILE NAME IS REQUIRED IS NOT PROVIDED"
	const val WORK_DATA_SAVE_EDITED_ITEM_FILE_URI_NOT_PROVIDED =
		"FILE URI IS REQUIRED TO COPY THE CONTENTS OF THE FILE"
	const val WORK_DATA_SAVE_EDITED_ITEM_FILE_INVALID = "WRONG URI PROVIDED SHOULD BE A FILE"

}