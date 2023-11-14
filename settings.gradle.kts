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
            version(
                "android.application",
                "8.1.3"
            )
            version(
                "kotlin",
                "1.9.20"
            )
            version(
                "kotlin.coroutines",
                "1.7.3"
            )
            version(
                "kotlin.ksp",
                "1.9.20-1.0.14"
            )
            plugin(
                "android.application",
                "com.android.application"
            ).versionRef("android.application")
            plugin(
                "kotlin",
                "org.jetbrains.kotlin.android"
            ).versionRef("kotlin")
            plugin(
                "ksp",
                "com.google.devtools.ksp"
            ).versionRef("kotlin.ksp")
            library(
                "kotlin.stdlib",
                "org.jetbrains.kotlin",
                "kotlin-stdlib"
            ).versionRef("kotlin")
            library(
                "kotlin.reflect",
                "org.jetbrains.kotlin",
                "kotlin-reflect"
            ).versionRef("kotlin")
            library(
                "kotlin.coroutines.core",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core-jvm"
            ).versionRef("kotlin.coroutines")
            library(
                "kotlin.coroutines.android",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-android"
            ).versionRef("kotlin.coroutines")
        }
    }
}

rootProject.name = "sketches"
include(":app")
