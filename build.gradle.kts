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
    alias(libs.plugins.spotless)
}

val ktlintVersion = libs.versions.ktlint.get()

// Kotlin formatting runs through ktlint's own cli rather than spotless's ktlint step.
//
// Spotless resolves .editorconfig differently from ktlint itself: with ktlint_code_style set to
// android_studio it was still applying ktlint_official-only rules, so `Reformat Code` in the IDE
// and the build disagreed about chained calls and could never both be satisfied.  The cli reads the
// same .editorconfig the IDE's ktlint plugin does, which makes all three agree by construction.
//
// Spotless is still what handles whitespace, xml and the odds and ends.
allprojects {
    val ktlint = configurations.create("ktlint")

    dependencies {
        ktlint("com.pinterest.ktlint:ktlint-cli:$ktlintVersion") {
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            }
        }
    }

    // build output is excluded here as well as in .editorconfig, so ktlint does not walk it at all
    val ktlintTargets = listOf("src/**/*.kt", "*.gradle.kts", "!**/build/**")

    tasks.register<JavaExec>("ktlintCheck") {
        group = "verification"
        description = "Checks Kotlin formatting with ktlint."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        workingDir = projectDir
        args = listOf("--relative") + ktlintTargets
    }

    tasks.register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fixes Kotlin formatting with ktlint."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        workingDir = projectDir
        // ktlint exits non-zero for anything it could not fix on its own
        args = listOf("--relative", "--format") + ktlintTargets
    }

    plugins.withId("base") {
        tasks.named("check") { dependsOn("ktlintCheck") }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("*.md", ".gitignore", ".editorconfig")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
