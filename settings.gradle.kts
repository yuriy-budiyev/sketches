pluginManagement {
    repositories {
        google()
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
    versionCatalogs {
        create("sketches") {
            from(files("gradle/sketches.versions.toml"))
        }
    }
}

rootProject.name = "sketches"
include(":app")
include(":core:constraints")
include(":core:data")
