import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.github.yuriybudiyev.sketches.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.bundles.kotlin)
    compileOnly(libs.kotlin.gradle)
    compileOnly(libs.kotlin.ksp.gradle)
    compileOnly(libs.kotlin.compose.gradle)
    compileOnly(libs.agp)
    compileOnly(libs.android.tools.common)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("SketchesApplication") {
            id = "sketches.plugins.application"
            implementationClass = "com.github.yuriybudiyev.sketches.ApplicationConventionPlugin"
        }
        register("SketchesFeature") {
            id = "sketches.plugins.feature"
            implementationClass = "com.github.yuriybudiyev.sketches.FeatureConventionPlugin"
        }
        register("SketchesLibrary") {
            id = "sketches.plugins.library"
            implementationClass = "com.github.yuriybudiyev.sketches.LibraryConventionPlugin"
        }
    }
}
