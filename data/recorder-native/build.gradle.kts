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
				cppFlags += ""
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
