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
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
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
