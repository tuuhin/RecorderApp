plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
}

android {
	namespace = "com.eva.bookmarks"
	compileSdk = 35

	defaultConfig {
		minSdk = 29

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
	kotlinOptions {
		jvmTarget = "17"
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)
	implementation(libs.kotlinx.datetime)

	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))
	implementation(project(":data:database"))
}