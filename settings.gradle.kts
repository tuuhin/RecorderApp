pluginManagement {
	includeBuild("build-logic")
	repositories {
		google {
			content {
				includeGroupByRegex("com\\.android.*")
				includeGroupByRegex("com\\.google.*")
				includeGroupByRegex("androidx.*")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}
}
dependencyResolutionManagement {
	repositories {
		google()
		mavenCentral()
	}
	versionCatalogs{
		create("ktorLibs") {
			from("io.ktor:ktor-version-catalog:3.5.0")
		}
	}
}

rootProject.name = "RecorderApp"
include(":app")
include(":data:interactions")
include(":data:location")
include(":core:utils")
include(":data:datastore")
include(":data:database")
include(":data:player")
include(":data:recordings")
include(":data:recorder")
include(":data:worker")
include(":data:bookmarks")
include(":data:categories")
include(":data:use_case")
include(":feature:recorder")
include(":core:ui")
include(":feature:settings")
include(":feature:categories")
include(":feature:recordings")
include(":feature:player")
include(":feature:widget")
include(":feature:editor")
include(":data:editor")
include(":feature:player-shared")
include(":feature:onboarding")
include(":testing:runtime")
include(":data:visualizer")
include(":data:transcribe")
