import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.jacoco)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOptions["bundleId"] = "com.meneses.budgethunter.ComposeApp"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Network
            implementation(libs.bundles.ktor)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Database (common parts only)
            implementation(libs.sqldelight.coroutines.extensions)

            // Koin
            implementation(libs.bundles.koin)

            // Lifecycle ViewModels
            implementation(libs.jetbrains.lifecycle.viewmodel)

            // DataStore (KMP support)
            implementation(libs.bundles.datastore)

            // Lottie animations
            implementation(libs.compottie)
            implementation(libs.compottie.resources)

            // Navigation Compose (multiplatform support)
            implementation(libs.jetbrains.navigation.compose)
        }

        androidMain.dependencies {
            // Android-specific dependencies
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.bundles.koin.android)

            // Navigation Compose (Android only for now)
            implementation(libs.androidx.navigation.compose)

            // Lifecycle (Android only for now)
            implementation(libs.bundles.androidx.lifecycle)

            // SqlDelight Android driver
            implementation(libs.sqldelight.android.driver)

            // Google Play services
            implementation(libs.bundles.play.update)

            // Ktor Android engine for HTTP calls
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            // SqlDelight iOS driver
            implementation(libs.sqldelight.ios.driver)

            // Ktor Darwin engine for iOS
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}

android {
    namespace = "com.meneses.budgethunter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.meneses.budgethunter"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"

        val instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunner = instrumentationRunner

        vectorDrawables {
            useSupportLibrary = true
        }

        // Load API key from local.properties
        val props = Properties()
        val propsFile = rootProject.file("local.properties")
        if (propsFile.exists()) {
            props.load(propsFile.inputStream())
        }

        val geminiApiKey = props.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.meneses.budgethunter.db")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    debugImplementation(libs.bundles.test.debug)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

// Jacoco configuration for code coverage
jacoco {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("testDebugUnitTestCoverage") {
    dependsOn("testDebugUnitTest")

    val buildDir = layout.buildDirectory.asFile.get()
    val coverageSourceDirs = listOf(
        "src/androidMain/kotlin",
        "src/commonMain/kotlin"
    )

    val classFilesTree = fileTree(
        mapOf(
            "dir" to "$buildDir/intermediates/javac/debug",
            "includes" to listOf("**/*.class"),
            "excludes" to listOf(
                "**/R.class",
                "**/R${'$'}.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "android/**/*.*"
            )
        )
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files(coverageSourceDirs))
    classDirectories.setFrom(classFilesTree)
    executionData.setFrom(file("$buildDir/jacoco/testDebugUnitTest.exec"))
}

// Ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("0.49.1")
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("**/build/generated/**")
        exclude { it.file.absolutePath.contains("/build/generated/") }
    }
}
