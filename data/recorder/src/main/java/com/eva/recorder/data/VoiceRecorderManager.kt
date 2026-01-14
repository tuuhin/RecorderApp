package com.eva.recorder.data

import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.recorder.domain.recorder.VoiceRecorder

class VoiceRecorderManager(
	private val recorder: VoiceRecorder,
	private val visualizer: AudioVisualDataProvider,
	private val transcriber: TranscriptionProvider
) : VoiceRecorder by recorder, AudioVisualDataProvider by visualizer,
	TranscriptionProvider by transcriber