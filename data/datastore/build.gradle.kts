plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)

	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.google.protobuf)
}

android {
	namespace = "com.eva.datastore"
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

	implementation(libs.androidx.datastore)
	implementation(libs.protobuf.javalite)
	implementation(libs.protobuf.kotlin.lite)

	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)

	implementation(project(":core:utils"))
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