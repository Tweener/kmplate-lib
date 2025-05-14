plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.jetbrains.compose).apply(false)
    alias(libs.plugins.jetbrains.compose.compiler).apply(false)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.maven.publish).apply(false)
    alias(libs.plugins.dependency.versions) // ./gradlew dependencyUpdates
}

tasks.register<gradle.RenameProjectTask>("renameProject")
