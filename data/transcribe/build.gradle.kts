plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.transcribe"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	ndkVersion = libs.versions.android.ndk.get()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		consumerProguardFiles("consumer-rules.pro")

		ndk {
			abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
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