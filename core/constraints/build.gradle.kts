plugins {
    alias(sketches.plugins.android.library)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.core.constraints"
    compileSdk = 34

    compileOptions {
        sourceCompatibility(sketches.versions.java.get())
        targetCompatibility(sketches.versions.java.get())
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    constraints {
        configurations
            .filter { it.isCanBeDeclared }
            .forEach {
                val configurationName = it.name
                add(
                    configurationName,
                    sketches.bundles.kotlin
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.annotation
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.core
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.arch.core
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.collection
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.concurrent
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.lifecycle
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.activity
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.compose
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.navigation
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.window
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.work
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.media3
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.tracing
                )
                add(
                    configurationName,
                    sketches.bundles.hilt
                )
                add(
                    configurationName,
                    sketches.bundles.coil
                )
                add(
                    configurationName,
                    sketches.androidx.versionedparcelable
                )
                add(
                    configurationName,
                    sketches.androidx.startup
                )
                add(
                    configurationName,
                    sketches.okhttp
                )
                add(
                    configurationName,
                    sketches.okio
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.compose.tooling
                )
                add(
                    configurationName,
                    sketches.androidx.compose.ui.test.manifest
                )
                add(
                    configurationName,
                    sketches.bundles.kotlin.test
                )
                add(
                    configurationName,
                    sketches.hilt.test
                )
                add(
                    configurationName,
                    sketches.junit
                )
                add(
                    configurationName,
                    sketches.bundles.androidx.test
                )
                add(
                    configurationName,
                    sketches.androidx.navigation.test
                )
                add(
                    configurationName,
                    sketches.androidx.compose.ui.test.junit
                )
            }
    }
}
