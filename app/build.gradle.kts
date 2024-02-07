plugins {
    alias(sketches.plugins.android.application)
    alias(sketches.plugins.kotlin)
    alias(sketches.plugins.kotlin.ksp)
    alias(sketches.plugins.hilt)
}

android {
    namespace = "com.github.yuriybudiyev.sketches"

    defaultConfig {
        applicationId = "com.github.yuriybudiyev.sketches"
        minSdk = 26
        targetSdk = 34
        compileSdk = 34
        buildToolsVersion = "34.0.0"
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
    implementation(project(":core:constraints"))
    implementation(project(":core:data"))
    implementation(sketches.bundles.kotlin)
    implementation(sketches.bundles.androidx.annotation)
    implementation(sketches.bundles.androidx.core)
    implementation(sketches.bundles.androidx.arch.core)
    implementation(sketches.bundles.androidx.collection)
    implementation(sketches.bundles.androidx.concurrent)
    implementation(sketches.bundles.androidx.lifecycle)
    implementation(sketches.bundles.androidx.activity)
    implementation(sketches.bundles.androidx.compose)
    implementation(sketches.bundles.androidx.navigation)
    implementation(sketches.bundles.androidx.window)
    implementation(sketches.bundles.androidx.work)
    implementation(sketches.bundles.androidx.media3)
    implementation(sketches.bundles.androidx.tracing)
    implementation(sketches.bundles.hilt)
    implementation(sketches.bundles.coil)
    implementation(sketches.androidx.versionedparcelable)
    implementation(sketches.androidx.startup)
    implementation(sketches.okhttp)
    implementation(sketches.okio)
    ksp(sketches.bundles.hilt.compiler)
    debugImplementation(sketches.bundles.androidx.compose.tooling)
    debugImplementation(sketches.androidx.compose.ui.test.manifest)
    testImplementation(sketches.bundles.kotlin.test)
    testImplementation(sketches.hilt.test)
    testImplementation(sketches.junit)
    kspTest(sketches.bundles.hilt.compiler)
    androidTestImplementation(sketches.bundles.kotlin.test)
    androidTestImplementation(sketches.bundles.androidx.test)
    androidTestImplementation(sketches.androidx.navigation.test)
    androidTestImplementation(sketches.androidx.compose.ui.test.junit)
    androidTestImplementation(sketches.hilt.test)
    kspAndroidTest(sketches.bundles.hilt.compiler)
}
