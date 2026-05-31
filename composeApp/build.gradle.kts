import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.compottie)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.mochi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mochi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Onde a classe Res (acesso a composeResources/) é gerada.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.mochi.resources"
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.mochi.db")
        }
    }
}

// ----- Code quality -----

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin",
    )
}
