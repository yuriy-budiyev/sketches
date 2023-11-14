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
            version("application", "8.1.3")
            version("kotlin", "1.9.20")
            version("coroutines", "1.7.3")
            version("ksp", "1.9.20-1.0.14")
            plugin("application", "com.android.application").versionRef("application")
            plugin("kotlin", "org.jetbrains.kotlin.android").versionRef("kotlin")
            plugin("ksp","com.google.devtools.ksp").versionRef("ksp")

        }
    }
}

rootProject.name = "sketches"
include(":app")
