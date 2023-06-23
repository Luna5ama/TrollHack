import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.luna5ama"

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
    id("dev.luna5ama.kmogus-struct-plugin") version "1.0.0-SNAPSHOT"
}

repositories {
    maven("https://maven.luna5ama.dev/")
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf(
                "-Xlambdas=indy",
                "-Xjvm-default=all",
                "-Xbackend-threads=0"
            )
        }
    }
}