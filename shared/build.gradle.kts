plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.compose)
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.sqlDelight)
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += "-Xmulti-platform"
            }
        }
    }


    /*listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }*/

    val sqlDelightVersion = "2.0.0"
    val preComposeVersion = "1.5.7"

    sourceSets {
        commonMain.dependencies {
            api("app.cash.sqldelight:coroutines-extensions:$sqlDelightVersion")
            implementation("app.cash.sqldelight:runtime:$sqlDelightVersion")
            api("moe.tlaster:precompose:$preComposeVersion")
            api("moe.tlaster:precompose-viewmodel:$preComposeVersion")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("junit:junit:4.13.2")
            implementation("io.mockk:mockk:1.13.5")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
        }

        androidMain.dependencies {
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.animation)
            implementation("app.cash.sqldelight:android-driver:$sqlDelightVersion")
            implementation("com.google.android.play:app-update:2.1.0")
            implementation("com.google.android.play:app-update-ktx:2.1.0")
            implementation("com.airbnb.android:lottie-compose:6.0.0")
            implementation("com.maxkeppeler.sheets-compose-dialogs:core:1.0.3")
            implementation("com.maxkeppeler.sheets-compose-dialogs:calendar:1.0.3")
        }

        /*iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:$sqlDelightVersion")
        }*/
    }
}

android {
    namespace = "com.meneses.budgethunter"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.meneses.budgethunter.db")
        }
    }
}
