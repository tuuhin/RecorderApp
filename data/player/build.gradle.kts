plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
}

android {
	namespace = "com.eva.player"
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
	// media3
	implementation(libs.androidx.media3.common)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.session)

	// futures to coroutine
	implementation(libs.androidx.concurrent.futures.ktx)
	implementation(libs.kotlinx.datetime)

	// hilt
	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)

	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))
	implementation(project(":data:datastore"))
}