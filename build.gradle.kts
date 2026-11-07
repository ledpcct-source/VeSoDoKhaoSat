plugins {
    // Kotlin Multiplatform & Compose plugins
    kotlin("multiplatform").version("2.1.0").apply(false)
    id("org.jetbrains.compose").version("1.7.3").apply(false)
    id("org.jetbrains.kotlin.plugin.compose").version("2.1.0").apply(false)
    id("com.android.application").version("8.8.0").apply(false)
    id("com.android.library").version("8.8.0").apply(false)
    kotlin("plugin.serialization").version("2.1.0").apply(false)
}

tasks.register("run") {
    group = "application"
    description = "Runs the Desktop application"
    dependsOn(":composeApp:run")
}

