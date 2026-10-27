package com.eva.recorder_native

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class FlacAudioRecorderTest {

	@Test
	fun companionInitLoadsCustomFlacLibrary() {
		val recorder = FlacAudioRecorder()

		assertNotNull(recorder)
	}
}
