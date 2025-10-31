package com.eva.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.eva.datastore.domain.DataStoreProvider
import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTests {

	@Inject
	lateinit var recorderRepo: RecorderAudioSettingsRepo

	@Inject
	lateinit var filesRepo: RecorderFileSettingsRepo

	@Inject
	lateinit var preferencesRepo: PreferencesSettingsRepo

	@Inject
	lateinit var provider: DataStoreProvider

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@BeforeTest
	fun setUp() = hiltRule.inject()

	@AfterTest
	fun tearDown() = runBlocking {
		provider.cleanUp()
	}

	@Test
	fun run_basic_updates_in_audio_settings() = runTest {
		recorderRepo.audioSettingsFlow.test(timeout = 5.seconds) {

			// initial update
			awaitItem()

			recorderRepo.onEncoderChange(RecordingEncoders.AMR_NB)
			assertEquals(
				expected = RecordingEncoders.AMR_NB,
				actual = awaitItem().encoders,
				message = "Encoder should be amr nb"
			)

			recorderRepo.onEncoderChange(RecordingEncoders.ACC)
			assertEquals(
				expected = RecordingEncoders.ACC,
				actual = awaitItem().encoders,
				message = "Encoder should be aac"
			)

			recorderRepo.onQualityChange(RecordQuality.HIGH)
			assertEquals(
				expected = RecordQuality.HIGH,
				actual = awaitItem().quality,
				message = "Quality is set to high"
			)

			recorderRepo.onQualityChange(RecordQuality.LOW)
			assertEquals(
				expected = RecordQuality.LOW,
				awaitItem().quality,
				message = "Quality is set to low"
			)

			cancelAndIgnoreRemainingEvents()
		}
	}

	@Test
	fun run_basic_updates_in_file_settings() = runTest {
		filesRepo.fileSettingsFlow.test(timeout = 5.seconds) {

			// skip the first
			awaitItem()

			filesRepo.onAllowExternalFileRead(true)
			assertEquals(
				true,
				awaitItem().allowExternalRead,
				"External files read is enabled"
			)

			filesRepo.onAllowExternalFileRead(false)
			assertEquals(
				false,
				awaitItem().allowExternalRead,
				"External files read is disabled"
			)

			filesRepo.onFileNameFormatChange(AudioFileNamingFormat.COUNT)
			assertEquals(
				expected = AudioFileNamingFormat.COUNT,
				actual = awaitItem().format,
				message = "Format should be count"
			)

			filesRepo.onFileNameFormatChange(AudioFileNamingFormat.DATE_TIME)
			assertEquals(
				expected = AudioFileNamingFormat.DATE_TIME,
				actual = awaitItem().format,
				message = "Format should be date-time"
			)

			cancelAndIgnoreRemainingEvents()
		}
	}

	@Test
	fun check_if_basic_preferences_with_onboarding_working() = runTest {
		preferencesRepo.canShowOnBoardingScreenFlow.test(timeout = 5.seconds) {

			awaitItem()

			preferencesRepo.updateCanShowOnBoarding(false)
			assertEquals(expected = false, actual = awaitItem(), "Should be false")

			preferencesRepo.updateCanShowOnBoarding(true)
			assertEquals(expected = true, actual = awaitItem(), "Should be true")

			cancelAndIgnoreRemainingEvents()
		}
	}
}