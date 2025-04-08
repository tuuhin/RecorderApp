plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.ui"
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
	buildFeatures {
		compose = true
	}
}

composeCompiler {

//	featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
	reportsDestination = project.layout.buildDirectory.dir("compose_compiler")
	metricsDestination = project.layout.buildDirectory.dir("compose_compiler")

	stabilityConfigurationFiles.add(
		rootProject.layout.projectDirectory.file("stability_config.conf")
	)
}

dependencies {

	// core
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.activity.compose)

	//compose
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)

	// navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.kotlinx.serialization.json)
	//dynamic font
	implementation(libs.androidx.ui.text.google.fonts)

	//kotlinx immutable
	api(libs.kotlinx.collections.immutable)
	//shapes
	api(libs.androidx.graphics.shapes)
}