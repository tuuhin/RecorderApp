plugins {
    alias(libs.plugins.recorderapp.android.library)
    alias(libs.plugins.recorderapp.hilt)
}

android {
    namespace = "com.eva.compression"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20")
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
