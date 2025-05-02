package com.eva.editor.domain.model

enum class AudioEditAction {
	/**Crop the audio based on [AudioClipConfig]
	 */
	CROP,

	/** Cut the section of audio  based on [AudioClipConfig]
	 */
	CUT,
}