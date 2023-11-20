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

            version(
                "android.application",
                "8.1.4"
            )
            version(
                "java",
                "1.8"
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
            bundle(
                "androidx.core",
                listOf(
                    "androidx.core",
                    "androidx.core.ktx"
                )
            )

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
            bundle(
                "androidx.annotation",
                listOf(
                    "androidx.annotation",
                    "androidx.annotation.jvm"
                )
            )

            version(
                "androidx.collection",
                "1.3.0"
            )
            library(
                "androidx.collection",
                "androidx.collection",
                "collection"
            ).versionRef("androidx.collection")
            library(
                "androidx.collection.ktx",
                "androidx.collection",
                "collection-ktx"
            ).versionRef("androidx.collection")
            bundle(
                "androidx.collection",
                listOf(
                    "androidx.collection",
                    "androidx.collection.ktx"
                )
            )

            version(
                "androidx.concurrent",
                "1.1.0"
            )
            library(
                "androidx.concurrent.futures",
                "androidx.concurrent",
                "concurrent-futures"
            ).versionRef("androidx.concurrent")
            library(
                "androidx.concurrent.futures.ktx",
                "androidx.concurrent",
                "concurrent-futures-ktx"
            ).versionRef("androidx.concurrent")
            bundle(
                "androidx.concurrent",
                listOf(
                    "androidx.concurrent.futures",
                    "androidx.concurrent.futures.ktx"
                )
            )

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
            bundle(
                "androidx.appcompat",
                listOf(
                    "androidx.appcompat",
                    "androidx.appcompat.resources"
                )
            )

            version(
                "androidx.compose.compiler",
                "1.5.4"
            )

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
            library(
                "androidx.compose.ui.test.junit",
                "androidx.compose.ui",
                "ui-test-junit4"
            ).versionRef("androidx.compose.ui")
            bundle(
                "androidx.compose.ui",
                listOf(
                    "androidx.compose.ui",
                    "androidx.compose.ui.geometry",
                    "androidx.compose.ui.graphics"
                )
            )
            bundle(
                "androidx.compose.ui.tooling",
                listOf(
                    "androidx.compose.ui.tooling",
                    "androidx.compose.ui.tooling.data",
                    "androidx.compose.ui.tooling.preview"
                )
            )

            version(
                "androidx.compose.foundation",
                "1.5.4"
            )
            library(
                "androidx.compose.foundation",
                "androidx.compose.foundation",
                "foundation"
            ).versionRef("androidx.compose.foundation")
            library(
                "androidx.compose.foundation.layout",
                "androidx.compose.foundation",
                "foundation-layout"
            ).versionRef("androidx.compose.foundation")
            bundle(
                "androidx.compose.foundation",
                listOf(
                    "androidx.compose.foundation",
                    "androidx.compose.foundation.layout"
                )
            )

            version(
                "androidx.compose.material",
                "1.5.4"
            )
            library(
                "androidx.compose.material",
                "androidx.compose.material",
                "material"
            ).versionRef("androidx.compose.material")
            library(
                "androidx.compose.material.icons.core",
                "androidx.compose.material",
                "material-icons-core"
            ).versionRef("androidx.compose.material")
            library(
                "androidx.compose.material.icons.extended",
                "androidx.compose.material",
                "material-icons-extended"
            ).versionRef("androidx.compose.material")
            library(
                "androidx.compose.material.ripple",
                "androidx.compose.material",
                "material-ripple"
            ).versionRef("androidx.compose.material")
            bundle(
                "androidx.compose.material",
                listOf(
                    "androidx.compose.material",
                    "androidx.compose.material.icons.core",
                    "androidx.compose.material.icons.extended",
                    "androidx.compose.material.ripple"
                )
            )

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
            bundle(
                "androidx.compose.material3",
                listOf(
                    "androidx.compose.material3",
                    "androidx.compose.material3.wsc"
                )
            )

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
            bundle(
                "androidx.activity",
                listOf(
                    "androidx.activity",
                    "androidx.activity.compose",
                    "androidx.activity.ktx"
                )
            )

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
            bundle(
                "androidx.fragment",
                listOf(
                    "androidx.fragment",
                    "androidx.fragment.ktx"
                )
            )

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
                "lifecycle-common"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.common.java8",
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
                "androidx.lifecycle.viewmodel.compose",
                "androidx.lifecycle",
                "lifecycle-viewmodel-compose"
            ).versionRef("androidx.lifecycle")
            library(
                "androidx.lifecycle.viewmodel.ktx",
                "androidx.lifecycle",
                "lifecycle-viewmodel-ktx"
            ).versionRef("androidx.lifecycle")
            bundle(
                "androidx.lifecycle",
                listOf(
                    "androidx.lifecycle.common",
                    "androidx.lifecycle.common.java8",
                    "androidx.lifecycle.runtime",
                    "androidx.lifecycle.runtime.ktx",
                    "androidx.lifecycle.livedata",
                    "androidx.lifecycle.livedata.ktx",
                    "androidx.lifecycle.livedata.core",
                    "androidx.lifecycle.livedata.core.ktx",
                    "androidx.lifecycle.viewmodel",
                    "androidx.lifecycle.viewmodel.savedstate",
                    "androidx.lifecycle.viewmodel.compose",
                    "androidx.lifecycle.viewmodel.ktx"
                )
            )

            version(
                "androidx.camera",
                "1.3.0"
            )
            library(
                "androidx.camera.core",
                "androidx.camera",
                "camera-core"
            ).versionRef("androidx.camera")
            library(
                "androidx.camera.camera2",
                "androidx.camera",
                "camera-camera2"
            ).versionRef("androidx.camera")
            library(
                "androidx.camera.video",
                "androidx.camera",
                "camera-video"
            ).versionRef("androidx.camera")
            library(
                "androidx.camera.view",
                "androidx.camera",
                "camera-view"
            ).versionRef("androidx.camera")
            library(
                "androidx.camera.lifecycle",
                "androidx.camera",
                "camera-lifecycle"
            ).versionRef("androidx.camera")
            library(
                "androidx.camera.extensions",
                "androidx.camera",
                "camera-extensions"
            ).versionRef("androidx.camera")
            bundle(
                "androidx.camera",
                listOf(
                    "androidx.camera.core",
                    "androidx.camera.camera2",
                    "androidx.camera.video",
                    "androidx.camera.view",
                    "androidx.camera.lifecycle",
                    "androidx.camera.extensions"
                )
            )

            version(
                "androidx.window",
                "1.2.0"
            )
            library(
                "androidx.window",
                "androidx.window",
                "window"
            ).versionRef("androidx.window")
            library(
                "androidx.window.core",
                "androidx.window",
                "window-core"
            ).versionRef("androidx.window")
            library(
                "androidx.window.java",
                "androidx.window",
                "window-java"
            ).versionRef("androidx.window")
            library(
                "androidx.window.testing",
                "androidx.window",
                "window-testing"
            ).versionRef("androidx.window")
            bundle(
                "androidx.window",
                listOf(
                    "androidx.window",
                    "androidx.window.core",
                    "androidx.window.java"
                )
            )

            version(
                "androidx.test.junit",
                "1.1.5"
            )
            library(
                "androidx.test.junit",
                "androidx.test.ext",
                "junit"
            ).versionRef("androidx.test.junit")

            version(
                "androidx.test.espresso",
                "3.5.1"
            )
            library(
                "androidx.test.espresso",
                "androidx.test.espresso",
                "espresso-core"
            ).versionRef("androidx.test.espresso")

            version(
                "okhttp",
                "4.11.0"
            )
            library(
                "okhttp",
                "com.squareup.okhttp3",
                "okhttp"
            ).versionRef("okhttp")

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
                "glide.okhttp",
                "com.github.bumptech.glide",
                "okhttp3-integration"
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
                    "glide.okhttp"
                )
            )

            version(
                "glide.compose",
                "1.0.0-beta01"
            )
            library(
                "glide.compose",
                "com.github.bumptech.glide",
                "compose"
            ).versionRef("glide.compose")

            version(
                "junit",
                "4.13.2"
            )
            library(
                "junit",
                "junit",
                "junit"
            ).versionRef("junit")

        }
    }
}

rootProject.name = "sketches"
include(":app")
