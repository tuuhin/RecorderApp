plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.transcribe"
    compileSdk {
        version = release(libs.versions.android.compileSdk.major.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdk.revision.get().toInt()
        }
    }
	ndkVersion = libs.versions.android.ndk.get()

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
	implementation(ktorLibs.client.cio)
	implementation(ktorLibs.client.logging)
	// okio
	implementation(libs.okio)
    implementation(libs.kotlinx.io.core)
	// serialization
	implementation(libs.kotlinx.serialization.json)
}
