plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.kotlinx.serialization)
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
	// vosk-model
	implementation(libs.vosk.model.en)
	implementation(libs.vosk.android)

	// serialization
	implementation(libs.kotlinx.serialization.json)
}