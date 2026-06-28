import com.android.build.api.dsl.ApkSigningConfig
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.spotless)
}

android {
    compileSdk = 37
    namespace = "us.mikeandwan.photos"

    defaultConfig {
        manifestPlaceholders += mapOf(
            "auth0Domain" to "@string/auth0_domain",
            "auth0Scheme" to "@string/auth0_scheme",
        )
        applicationId = "us.mikeandwan.pictures"
        minSdk = 26
        targetSdk = 37
        versionCode = 96
        versionName = "9.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // https://developer.android.com/training/data-storage/room/migrating-db-versions#kotlin_2
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    signingConfigs {
        val homeDir = System.getProperty("user.home")
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream("$homeDir/.gradle/gradle.properties"))

        create("release") {
            storeFile = file(keystoreProperties["RELEASE_STORE_FILE"] as String)
            storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String
            keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
            keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
        }
    }

    flavorDimensions += "dev_or_prod"

    productFlavors {
        create("development") {
            dimension = "dev_or_prod"
        }

        create("production") {
            dimension = "dev_or_prod"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true
            signingConfig = signingConfigs["release"]

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-maw.pro",
            )

            // run manually for now - before running, ensure there is an authenticated session
            // baselineProfile.automaticGenerationDuringBuild = true
        }
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// The androidx.baselineprofile plugin derives the `benchmarkRelease` and `nonMinifiedRelease`
// build types from `release`, so they inherit the release signing config. Re-sign ONLY the
// development flavor's baseline-profile variants with the debug key: this lets a developer log in
// to a debug build and have generation install over it as a same-signature update, preserving the
// stored Auth0 session. Production baseline-profile builds keep the release signing so they remain
// install-compatible with the shipped release app instead of colliding with it (which would fail
// with INSTALL_FAILED_UPDATE_INCOMPATIBLE).
androidComponents {
    lateinit var debugSigning: ApkSigningConfig
    finalizeDsl { ext ->
        debugSigning = ext.signingConfigs.getByName("debug")
    }
    onVariants { variant ->
        val isBaselineBuildType =
            variant.buildType == "nonMinifiedRelease" || variant.buildType == "benchmarkRelease"
        if (variant.flavorName == "development" && isBaselineBuildType) {
            variant.signingConfig.setConfig(debugSigning)
        }
    }
}

// Force kotlin-metadata-jvm to match the Kotlin version so Hilt's bundled (older) version
// doesn't reject the metadata format produced by the current Kotlin compiler.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
    }
}

dependencies {
    implementation(libs.androidx.profileinstaller)

    baselineProfile(project(":baselineprofile"))

    implementation(libs.jetbrains.kotlin.stdlib)
    implementation(libs.jetbrains.coroutines.android)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(libs.jetbrains.kotlinx.datetime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.okhttp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.room)
    implementation(libs.androidx.work)

    ksp(libs.androidx.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.google.hilt)
    ksp(libs.google.hilt.android.compiler)

    implementation(libs.auth0)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(libs.compose.ratingbar)
    implementation(libs.flowext)
    implementation(libs.markdown)
    implementation(libs.markdown.material)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.timber)
    implementation(libs.zoomable)

    testImplementation(libs.junit)
    testImplementation(libs.google.hilt.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.jetbrains.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

hilt {
    enableAggregatingTask = true
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("xml") {
        target("src/**/*.xml")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    format("misc") {
        target("*.md", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
