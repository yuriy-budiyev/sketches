plugins {
    alias(sketches.plugins.android.application)
    alias(sketches.plugins.kotlin)
    alias(sketches.plugins.ksp)
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
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation(sketches.bundles.kotlin)
    implementation(sketches.bundles.androidx)
    implementation(sketches.bundles.androidx.compose)
    implementation(sketches.bundles.androidx.camera)
    implementation(sketches.okhttp)
    implementation(sketches.bundles.glide)
    ksp(sketches.glide.ksp)
    debugImplementation(sketches.bundles.androidx.compose.debug)
    testImplementation(sketches.junit)
    androidTestImplementation(sketches.bundles.androidx.test)
    androidTestImplementation(sketches.androidx.compose.ui.test.junit)
}
