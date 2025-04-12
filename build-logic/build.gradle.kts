plugins {
	`kotlin-dsl`
}

group = "com.eva.plugins"

dependencies {

	compileOnly(libs.android.gradlePlugin)
	compileOnly(libs.kotlin.gradlePlugin)
	compileOnly(libs.room.gradlePlugin)
	compileOnly(libs.ksp.gradlePlugin)
	compileOnly(libs.protobuf.gradlePlugin)
	compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
	plugins {
		register("androidRoom") {
			id = "recorderapp.room.plugin"
			implementationClass = "ConfigureRoomPlugin"
		}
		register("hilt") {
			id = "recorderapp.hilt.plugin"
			implementationClass = "ConfigureHiltPlugin"
		}
		register("androidLibrary") {
			id = "recorderapp.android.library.plugin"
			implementationClass = "ConfigureAndroidLibraryPlugin"
		}
		register("protobuf") {
			id = "recorderapp.protobuf.plugin"
			implementationClass = "ConfigureProtobufPlugin"
		}
		register("composeCompiler") {
			id = "recorderapp.compose.compiler.plugin"
			implementationClass = "ConfigureComposePlugin"
		}
	}
}