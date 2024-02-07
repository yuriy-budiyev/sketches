plugins {
    alias(sketches.plugins.android.library)
    alias(sketches.plugins.kotlin)
    alias(sketches.plugins.kotlin.ksp)
    alias(sketches.plugins.hilt)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.core.data"
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
    implementation(sketches.bundles.androidx.collection)
    implementation(sketches.bundles.hilt.library)
    ksp(sketches.bundles.hilt.compiler)
    testImplementation(sketches.bundles.kotlin.test)
    testImplementation(sketches.hilt.test)
    testImplementation(sketches.junit)
    kspTest(sketches.bundles.hilt.compiler)

}
