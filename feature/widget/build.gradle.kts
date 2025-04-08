plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.google.protobuf)
}

android {
	namespace = "com.eva.feature_widget"
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
	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	metricsDestination = layout.buildDirectory.dir("compose_compiler")

	stabilityConfigurationFiles.add(
		rootProject.layout.projectDirectory.file("stability_config.conf")
	)
}

dependencies {
	// core
	implementation(libs.androidx.core.ktx)

	//compose
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.material3)

	//hilt
	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)

	//glance
	implementation(libs.androidx.glance)
	implementation(libs.androidx.glance.appwidget)
	implementation(libs.androidx.glance.material3)
	implementation(libs.androidx.glance.preview)
	implementation(libs.androidx.glance.appwidget.preview)

	//datastore
	implementation(libs.androidx.datastore)
	implementation(libs.protobuf.javalite)
	implementation(libs.protobuf.kotlin.lite)

	//core
	implementation(project(":core:utils"))
	implementation(project(":core:ui"))
	//data
	implementation(project(":data:recorder"))
	implementation(project(":data:recordings"))
	implementation(project(":data:use_case"))
}

protobuf {
	protoc {
		artifact = libs.protobuf.protoc.get().toString()
	}
	plugins {
		create("java") {
			artifact = libs.protobuf.gen.javalite.get().toString()
		}
	}

	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				create("java") {
					option("lite")
				}
				create("kotlin") {
					option("lite")
				}
			}
		}
	}
}
