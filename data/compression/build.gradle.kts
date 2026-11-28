plugins {
    alias(libs.plugins.recorderapp.android.library)
    alias(libs.plugins.recorderapp.hilt)
}

android {
    namespace = "com.eva.compression"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }
        consumerProguardFiles("consumer-rules.keep")

        externalNativeBuild {
            cmake {
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
}
