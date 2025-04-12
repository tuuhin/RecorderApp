plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.use_case"
}

dependencies {
	implementation(project(":core:utils"))
	implementation(project(":data:interactions"))
	implementation(project(":data:recordings"))
	implementation(project(":data:datastore"))
	implementation(project(":data:categories"))
}
