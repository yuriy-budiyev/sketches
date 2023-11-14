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
        create("sketches") {

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
                "androidx.annotation",
                "1.7.0"
            )
            library(
                "androidx.annotation",
                "androidx.annotation",
                "annotation"
            ).versionRef("androidx.annotation")
            library(
                "androidx.annotation.jvm",
                "androidx.annotation",
                "annotation-jvm"
            ).versionRef("androidx.annotation")

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
            library(
                "androidx.compose.material3.wsc",
                "androidx.compose.material3",
                "material3-window-size-class"
            ).versionRef("androidx.compose.material3")

            version(
                "androidx.appcompat",
                "1.6.1"
            )
            library(
                "androidx.appcompat",
                "androidx.appcompat",
                "appcompat"
            ).versionRef("androidx.appcompat")
            library(
                "androidx.appcompat.resources",
                "androidx.appcompat",
                "appcompat-resources"
            ).versionRef("androidx.appcompat")

            version(
                "androidx.activity",
                "1.8.0"
            )
            library(
                "androidx.activity",
                "androidx.activity",
                "activity"
            ).versionRef("androidx.activity")
            library(
                "androidx.activity.compose",
                "androidx.activity",
                "activity-compose"
            ).versionRef("androidx.activity")
            library(
                "androidx.activity.ktx",
                "androidx.activity",
                "activity-ktx"
            ).versionRef("androidx.activity")

            version(
                "androidx.fragment",
                "1.6.2"
            )
            library(
                "androidx.fragment",
                "androidx.fragment",
                "fragment"
            ).versionRef("androidx.fragment")
            library(
                "androidx.fragment.ktx",
                "androidx.fragment",
                "fragment-ktx"
            ).versionRef("androidx.fragment")

            version(
                "androidx.lifecycle",
                "2.6.2"
            )
            library(
                "androidx.lifecycle.runtime",
                "androidx.lifecycle",
                "lifecycle-runtime"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.runtime.ktx",
                "androidx.lifecycle",
                "lifecycle-runtime-ktx"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.common",
                "androidx.lifecycle",
                "lifecycle-common-java8"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.livedata",
                "androidx.lifecycle",
                "lifecycle-livedata"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.livedata.ktx",
                "androidx.lifecycle",
                "lifecycle-livedata-ktx"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.livedata.core",
                "androidx.lifecycle",
                "lifecycle-livedata-core"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.livedata.core.ktx",
                "androidx.lifecycle",
                "lifecycle-livedata-core-ktx"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.viewmodel",
                "androidx.lifecycle",
                "lifecycle-viewmodel"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.viewmodel.savedstate",
                "androidx.lifecycle",
                "lifecycle-viewmodel-savedstate"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.viewmodel.ktx",
                "androidx.lifecycle",
                "lifecycle-viewmodel-ktx"
            ).versionRef("androidx.lifecycle")
            bundle(
                "androidx",
                listOf(
                    "androidx.core",
                    "androidx.core.ktx",
                    "androidx.annotation",
                    "androidx.annotation.jvm",
                    "androidx.appcompat",
                    "androidx.appcompat.resources",
                    "androidx.fragment",
                    "androidx.fragment.ktx",
                    "androidx.lifecycle.runtime",
                    "androidx.lifecycle.runtime.ktx",
                    "androidx.lifecycle.common",
                    "androidx.lifecycle.livedata",
                    "androidx.lifecycle.livedata.ktx",
                    "androidx.lifecycle.livedata.core",
                    "androidx.lifecycle.livedata.core.ktx",
                    "androidx.lifecycle.viewmodel",
                    "androidx.lifecycle.viewmodel.savedstate",
                    "androidx.lifecycle.viewmodel.ktx"
                )
            )
            bundle(
                "androidx.compose",
                listOf(
                    "androidx.compose.ui",
                    "androidx.compose.ui.geometry",
                    "androidx.compose.ui.graphics",
                    "androidx.compose.material3",
                    "androidx.compose.material3.wsc",
                    "androidx.activity",
                    "androidx.activity.compose",
                    "androidx.activity.ktx"
                )
            )
            bundle(
                "androidx.compose.debug",
                listOf(
                    "androidx.compose.ui.tooling",
                    "androidx.compose.ui.tooling.data",
                    "androidx.compose.ui.tooling.preview"
                )
            )

            // Glide
            version(
                "glide",
                "5.0.0-rc01"
            )
            library(
                "glide",
                "com.github.bumptech.glide",
                "glide"
            ).versionRef("glide")
            library(
                "glide.okhttp3",
                "com.github.bumptech.glide",
                "okhttp3-integration"
            ).versionRef("glide")
            library(
                "glide.compose",
                "com.github.bumptech.glide",
                "compose"
            ).versionRef("glide")
            library(
                "glide.ksp",
                "com.github.bumptech.glide",
                "ksp"
            ).versionRef("glide")
            bundle(
                "glide",
                listOf(
                    "glide",
                    "glide.okhttp3",
                    "glide.compose"
                )
            )
        }
    }
}

rootProject.name = "sketches"
include(":app")
