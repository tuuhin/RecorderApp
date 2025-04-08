plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.androidx.room)
	alias(libs.plugins.hilt)
}

android {
	namespace = "com.eva.database"
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

room {
	schemaDirectory("$projectDir/schemas")
}


dependencies {

	implementation(libs.androidx.core.ktx)

	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)

	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)
	implementation(libs.kotlinx.datetime)

}