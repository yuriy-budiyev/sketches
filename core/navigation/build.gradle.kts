plugins {
    alias(sketches.plugins.android.library)
    alias(sketches.plugins.kotlin)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.core.navigation"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    implementation(project(":core:constraints"))
    implementation(sketches.bundles.kotlin)
    implementation(sketches.bundles.androidx.navigation)
    testImplementation(sketches.bundles.kotlin.test)
    testImplementation(sketches.hilt.test)
    testImplementation(sketches.junit)
}
