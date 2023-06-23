rootProject.name = "TrollHack"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.luna5ama.dev/")
        maven("https://maven.fastmc.dev/")
    }

    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val kmogusVersion: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
        id("dev.luna5ama.kmogus-struct-plugin") version kmogusVersion
    }
}

include("codegen", "structs")