import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.hilt)
	alias(libs.plugins.ksp)
}

android {
	namespace = "com.eva.testing.runtime"

	compileSdk = libs.versions.android.compileSdk.get().toInt()
	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		testInstrumentationRunner = "com.eva.testing.runtime.HiltTestRunner"
		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

dependencies {
	implementation(libs.hilt.android)
	implementation(libs.hilt.test)
	implementation(libs.androidx.runner)
	ksp(libs.hilt.android.compiler)
}