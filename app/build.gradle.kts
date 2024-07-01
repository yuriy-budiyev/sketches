plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.github.yuriybudiyev.sketches"

    defaultConfig {
        applicationId = "com.github.yuriybudiyev.sketches"
        minSdk = 26
        targetSdk = 35
        compileSdk = 35
        buildToolsVersion = "35.0.0"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility(libs.versions.java.get())
        targetCompatibility(libs.versions.java.get())
    }

    kotlinOptions {
        jvmTarget = libs.versions.java.get()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.androidx.annotation)
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.androidx.arch.core)
    implementation(libs.bundles.androidx.collection)
    implementation(libs.bundles.androidx.concurrent)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.androidx.activity)
    implementation(libs.bundles.androidx.compose)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.bundles.androidx.window)
    implementation(libs.bundles.androidx.work)
    implementation(libs.bundles.androidx.media3)
    implementation(libs.bundles.androidx.tracing)
    implementation(libs.bundles.hilt)
    implementation(libs.bundles.coil)
    implementation(libs.okio)
    ksp(libs.bundles.hilt.compiler)
    debugImplementation(libs.bundles.androidx.compose.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.bundles.kotlin.test)
    testImplementation(libs.bundles.androidx.test)
    testImplementation(libs.hilt.test)
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.kotlin.test)
    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.androidx.navigation.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(libs.hilt.test)
}
