pluginManagement {
    repositories {
        maven("https://maven.luna5ama.dev/")
        gradlePluginPortal()
    }

    val kotlinVersion: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    }
}

rootProject.name = "structs"