package com.eva.recordings.domain.provider

import java.io.File

interface RecorderFileProvider {

	/**
	 * Creates a temp file for recording
	 * @param extension The file extension for the temp file to be created
	 * @return [java.io.File] the newly created file for recording.
	 */
	suspend fun createFileForRecording(extension: String? = null): File

	/**
	 * Reads the file data and transfer the metadata to the database and returns file id from the
	 * database if entry successfully entered
	 * @param file The File whose data to be copied or metadata need to be evaluated
	 * @param mimeType MimeType of the file to be submitted
	 * @return The recordingId assigned for the current recording
	 */
	suspend fun transferFileDataToStorage(file: File, mimeType: String): Long?


	/**
	 * Deletes the temporary recording file in-case the recording is cancelled
	 */
	suspend fun deleteCreatedFile(file: File): Boolean

}