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

        // App (project-level) version catalog
        create("scratches") {

            // Plugins and SDK
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
            bundle(
                "kotlin",
                listOf(
                    "kotlin.stdlib",
                    "kotlin.reflect",
                    "kotlin.coroutines.core",
                    "kotlin.coroutines.android"
                )
            )

            //Libraries
            // AndroidX
            version(
                "androidx.core",
                "1.12.0"
            )
            library(
                "androidx.core",
                "androidx.core",
                "core"
            ).versionRef("androidx.core")
            library(
                "androidx.core.ktx",
                "androidx.core",
                "core-ktx"
            ).versionRef("androidx.core")
            version(
                "androidx.compose.ui",
                "1.5.4"
            )
            library(
                "androidx.compose.ui",
                "androidx.compose.ui",
                "ui"
            ).versionRef("androidx.compose.ui")
            library(
                "androidx.compose.ui.geometry",
                "androidx.compose.ui",
                "ui-geometry"
            ).versionRef("androidx.compose.ui")
            library(
                "androidx.compose.ui.graphics",
                "androidx.compose.ui",
                "ui-graphics"
            ).versionRef("androidx.compose.ui")
            library(
                "androidx.compose.ui.tooling",
                "androidx.compose.ui",
                "ui-tooling"
            ).versionRef("androidx.compose.ui")
            library(
                "androidx.compose.ui.tooling.data",
                "androidx.compose.ui",
                "ui-tooling-data"
            ).versionRef("androidx.compose.ui")
            library(
                "androidx.compose.ui.tooling.preview",
                "androidx.compose.ui",
                "ui-tooling-preview"
            ).versionRef("androidx.compose.ui")
            version(
                "androidx.compose.material3",
                "1.1.2"
            )
            library(
                "androidx.compose.material3",
                "androidx.compose.material3",
                "material3"
            ).versionRef("androidx.compose.material3")
        }
    }
}

rootProject.name = "sketches"
include(":app")
