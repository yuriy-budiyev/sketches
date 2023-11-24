plugins {
    alias(sketches.plugins.android.application)
    alias(sketches.plugins.kotlin)
    alias(sketches.plugins.kotlin.ksp)
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
    implementation(sketches.bundles.androidx.core)
    implementation(sketches.bundles.androidx.annotation)
    implementation(sketches.bundles.androidx.collection)
    implementation(sketches.bundles.androidx.concurrent)
    implementation(sketches.bundles.androidx.appcompat)
    implementation(sketches.bundles.androidx.navigation)
    implementation(sketches.bundles.androidx.compose)
    implementation(sketches.bundles.androidx.activity)
    implementation(sketches.bundles.androidx.fragment)
    implementation(sketches.bundles.androidx.lifecycle)
    implementation(sketches.bundles.androidx.camera)
    implementation(sketches.bundles.androidx.window)
    implementation(sketches.bundles.hilt)
    implementation(sketches.bundles.glide)
    implementation(sketches.okhttp)
    implementation(sketches.androidsvg)

    ksp(sketches.bundles.hilt.compiler)
    ksp(sketches.glide.compiler)

    debugImplementation(sketches.bundles.androidx.compose.tooling)
}
