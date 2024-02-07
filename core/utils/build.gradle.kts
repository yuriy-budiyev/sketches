plugins {
    alias(sketches.plugins.android.library)
    alias(sketches.plugins.kotlin)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.core.utils"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility(sketches.versions.java.get())
        targetCompatibility(sketches.versions.java.get())
    }

    kotlinOptions {
        jvmTarget = sketches.versions.java.get()
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:constraints"))
    implementation(sketches.bundles.kotlin)
    testImplementation(sketches.bundles.kotlin.test)
    testImplementation(sketches.junit)
}
