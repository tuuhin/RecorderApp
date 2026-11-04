import com.android.build.api.dsl.ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES

plugins {
	alias(libs.plugins.android.test)
	alias(libs.plugins.androidx.baselineprofile)
}

android {
	namespace = "com.eva.recorderapp.baselineprofile"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetProjectPath = ":app"
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
	}

	testOptions {
		managedDevices {
			localDevices {
				create("pixel6Api37") {
					device = "Pixel 6"
					apiLevel = 37
					systemImageSource = "google_apis_playstore"
					pageAlignment = FORCE_16KB_PAGES
				}
			}
		}
	}
}

dependencies {
	implementation(libs.androidx.benchmark.macro.junit4)
	implementation(libs.androidx.uiautomator)
	implementation(libs.androidx.junit)
}

baselineProfile {
	useConnectedDevices = false
	managedDevices += "pixel6Api37"
}
