plugins {
    alias(sketches.plugins.android.test)
    alias(sketches.plugins.androidx.baselineprofile)
    alias(sketches.plugins.kotlin)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.baselineprofile"

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

    defaultConfig {
        minSdk = 28
        targetSdk = 34
        compileSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    testOptions.managedDevices.devices {
        create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api33") {
            device = "Pixel 6"
            apiLevel = 33
            systemImageSource = "aosp"
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    managedDevices += "pixel6Api33"
    useConnectedDevices = false
}

dependencies {
    implementation(sketches.bundles.kotlin)
    implementation(sketches.bundles.kotlin.test)
    implementation(sketches.bundles.androidx.annotation)
    implementation(sketches.bundles.androidx.test)
    implementation(sketches.bundles.androidx.tracing)
    implementation(sketches.androidx.benchmark.macro)
    implementation(sketches.guava)
    implementation(sketches.okio)
}
