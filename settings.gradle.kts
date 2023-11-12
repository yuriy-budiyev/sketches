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
        create("scratches") {
            version("kotlin", "1.9.20")
            version("coroutines", "1.7.3")
            plugin("android", "com.android.application").versionRef("kotlin")
        }
    }
}

rootProject.name = "sketches"
include(":app")
