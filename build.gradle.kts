// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            // Use ktlint version from the version catalog (gradle/libs.versions.toml)
            ktlint(libs.versions.ktlint.get())
            trimTrailingWhitespace()
            leadingTabsToSpaces(4)
            endWithNewline()
        }

        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            // Use ktlint for Kotlin Gradle files with the catalog version as well
            ktlint(libs.versions.ktlint.get())
            trimTrailingWhitespace()
            endWithNewline()
        }

        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**")
            trimTrailingWhitespace()
            leadingTabsToSpaces(4)
            endWithNewline()
        }

        format("misc") {
            target("**/*.md", "**/.gitignore")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
