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
            implementationClass = "ApplicationConventionPlugin"
        }
        register("SketchesFeature") {
            id = "sketches.plugins.feature"
            implementationClass = "FeatureConventionPlugin"
        }
        register("SketchesLibrary") {
            id = "sketches.plugins.library"
            implementationClass = "LibraryConventionPlugin"
        }
    }
}
