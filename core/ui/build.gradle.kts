plugins {
    alias(sketches.plugins.android.library)
    alias(sketches.plugins.kotlin)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.core.ui"
    compileSdk = 34

    defaultConfig {
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility(sketches.versions.java.get())
        targetCompatibility(sketches.versions.java.get())
    }

    kotlinOptions {
        jvmTarget = sketches.versions.java.get()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = sketches.versions.androidx.compose.compiler.get()
    }
}

dependencies {
    api(sketches.bundles.androidx.lifecycle)
    api(sketches.bundles.androidx.compose)
    api(sketches.bundles.androidx.media3)
    api(sketches.bundles.coil)
    implementation(project(":core:constraints"))
    implementation(project(":core:data"))
    implementation(sketches.bundles.kotlin)
    testImplementation(sketches.bundles.kotlin.test)
    testImplementation(sketches.hilt.test)
    testImplementation(sketches.junit)
}
