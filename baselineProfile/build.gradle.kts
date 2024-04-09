plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.github.yuriybudiyev.sketches.baselineprofile"
    compileSdk = 34

    compileOptions {
        sourceCompatibility(libs.versions.java.get())
        targetCompatibility(libs.versions.java.get())
    }

    kotlinOptions {
        jvmTarget = libs.versions.java.get()
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    targetProjectPath = ":app"

}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.kotlin.test)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.espresso)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()

        @Suppress("UnstableApiUsage")
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId }
        )
    }
}
