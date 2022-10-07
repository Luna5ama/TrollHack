pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/Luna5ama/JarOptimizer/maven-repo")
    }

    val kotlinVersion: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    }
}

rootProject.name = "TrollHack"