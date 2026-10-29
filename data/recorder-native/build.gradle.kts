plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.recorder_native"
	ndkVersion = libs.versions.android.ndk.get()

	defaultConfig {
		externalNativeBuild {
			cmake {
				cppFlags += listOf("-std=c++20")
				arguments += "-DCMAKE_SYSTEM_VERSION=${libs.versions.android.minSdk.get().toInt()}"
			}
		}
	}


	externalNativeBuild {
		cmake {
			path = file("src/main/cpp/CMakeLists.txt")
			version = "3.22.1"
		}
	}
}

dependencies {
	androidTestImplementation(libs.assertk)
	androidTestImplementation(libs.androidx.rules)
	androidTestImplementation(libs.androidx.media3.test.utils)
	androidTestImplementation(libs.androidx.media3.extractor)
}