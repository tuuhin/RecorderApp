plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.transcribe"

	defaultConfig {
		ndk {
			abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
		}
	}

	buildFeatures {
		buildConfig = true
	}
}

dependencies {
	implementation(libs.vosk.model.en)
	implementation(libs.vosk.android)
}