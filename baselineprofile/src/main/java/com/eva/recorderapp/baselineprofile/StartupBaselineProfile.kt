package com.eva.recorderapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupProfile() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        // press home to exit the app
        pressHome()
    }


    @Test
    fun userJourneyProfile() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = false,
    ) {

        pressHome()
        startActivityAndWait()

        device.findObject(By.text("Skip"))?.click()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_TIMEOUT_MS)
        // press home to exit the app
        pressHome()
    }

    companion object {
        private const val PACKAGE_NAME = "com.eva.recorderapp"
        private const val UI_TIMEOUT_MS = 5_000L
    }
}
