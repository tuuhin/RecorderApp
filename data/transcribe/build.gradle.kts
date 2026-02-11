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
	implementation(project(":core:utils"))
	implementation(project(":data:database"))
	// vosk-model
	implementation(libs.vosk.android)
	// ktor
	implementation(ktorLibs.client.core)
	implementation(ktorLibs.client.android)
	implementation(ktorLibs.client.logging)
	// okio
	implementation(libs.okio)
	// serialization
	implementation(libs.kotlinx.serialization.json)
}