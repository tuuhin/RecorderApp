package com.eva.recorderapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {

	@get:Rule
	val baselineProfileRule = BaselineProfileRule()

	@Test
	fun generate() = baselineProfileRule.collect(
		packageName = "com.eva.recorderapp.release",
		includeInStartupProfile = true,
	) {
		pressHome()
		startActivityAndWait()
	}
}
