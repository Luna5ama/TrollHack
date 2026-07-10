pluginManagement {
    val versions = file("gradle/libs.versions.toml").readLines()

    fun catalogVersion(alias: String): String {
        val key = "$alias = "
        return versions
            .dropWhile { it.trim() != "[versions]" }
            .drop(1)
            .takeWhile { !it.trim().startsWith("[") }
            .firstNotNullOfOrNull { line ->
                line.trim().takeIf { it.startsWith(key) }
                    ?.substringAfter('"')
                    ?.substringBefore('"')
            } ?: error("Missing version catalog entry: $alias")
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version catalogVersion("foojay")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "TrollHack"

include("fabric")
include("neoforge")
