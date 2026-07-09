import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    kotlin("jvm") version "2.1.0"
    id("fabric-loom") version "1.17.13"
    id("com.gradleup.shadow") version "8.3.6"
}

val modId = property("mod_id").toString()
val modName = property("mod_name").toString()
val modAuthor = property("mod_author").toString()
val modVersion = property("mod_version").toString()
val mavenGroup = property("maven_group").toString()
val archiveName = property("archives_base_name").toString()
val minecraftVersion = property("minecraft_version").toString()
val fabricLoaderVersion = property("fabric_loader_version").toString()
val fabricVersion = property("fabric_version").toString()
val license = property("license").toString()
val javaVersion = property("java_version").toString()
val reflectionsVersion = property("reflections_version").toString()
val skijaVersion = "0.116.4"

group = mavenGroup
version = modVersion
description = property("description").toString()

base {
    archivesName = archiveName
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://jitpack.io")
}

val library by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

loom {
    val accessWidener = file("src/main/resources/$modId.accesswidener")
    if (accessWidener.exists()) {
        accessWidenerPath.set(accessWidener)
    }
    mixin {
        defaultRefmapName.set("$modId.refmap.json")
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.layered {
        officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    implementation("org.ow2.asm:asm-tree:9.6")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")

    library("org.jetbrains.kotlin:kotlin-stdlib")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    library("org.reflections:reflections:$reflectionsVersion")
    library("com.squareup.okhttp3:okhttp:4.12.0")
    library("net.lenni0451:Reflect:1.3.4")
    library("org.luaj:luaj-jse:3.0.1")
    library("io.github.spair:imgui-java-binding:1.89.0")
    library("io.github.spair:imgui-java-lwjgl3:1.89.0") {
        exclude(group = "org.lwjgl")
    }
    library("io.github.spair:imgui-java-app:1.89.0") {
        exclude(group = "org.lwjgl")
    }
    library("io.github.humbleui:${skijaArtifact()}:$skijaVersion")
    library("org.lwjgl:lwjgl-assimp:3.3.4") {
        exclude("org.lwjgl", "lwjgl")
    }
    library("org.lwjgl", "lwjgl-assimp", "3.3.4", classifier = lwjglNatives()) {
        exclude("org.lwjgl", "lwjgl")
    }
    library("org.jcodec:jcodec:0.2.5")
    library("org.jcodec:jcodec-javase:0.2.5")
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
        compileClasspath += library
        runtimeClasspath += library
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
        optIn = listOf("kotlin.RequiresOptIn", "kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs = listOf(
            "-Xjvm-default=all-compatibility",
            "-Xcontext-receivers"
        )
    }
}

tasks.processResources {
    val expandProps = mapOf(
        "version" to version,
        "group" to project.group,
        "minecraft_version" to minecraftVersion,
        "fabric_version" to fabricVersion,
        "fabric_loader_version" to fabricLoaderVersion,
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "mod_id" to modId,
        "license" to license,
        "description" to project.description,
        "java_version" to javaVersion
    )
    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\n") else value
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$modName" }
    }
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to modAuthor,
            "Specification-Version" to archiveVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to archiveVersion,
            "Implementation-Vendor" to modAuthor,
            "Built-On-Minecraft" to minecraftVersion
        )
    }
}

tasks.shadowJar {
    archiveClassifier = "named"
    configurations = listOf(library)
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile = tasks.shadowJar.get().archiveFile
}

tasks.javadoc {
    enabled = false
}

tasks.matching { it.name == "javadocJar" || it.name == "sourcesJar" || it.name == "remapSourcesJar" }.configureEach {
    enabled = false
}

fun lwjglNatives(): String {
    val name = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    return when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) }) {
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            } else if (arch.startsWith("ppc")) {
                "natives-linux-ppc64le"
            } else if (arch.startsWith("riscv")) {
                "natives-linux-riscv64"
            } else {
                "natives-linux"
            }
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> "natives-macos-arm64"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64")) "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}" else "natives-windows-x86"
        else -> error("Unrecognized or unsupported platform. Please set lwjglNatives manually.")
    }
}

fun skijaArtifact(): String {
    val name = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    return when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arch.contains("64") && !arch.startsWith("aarch64") && !arch.startsWith("arm")) {
                "skija-linux-x64"
            } else {
                error("Unsupported Skija Linux architecture: $arch")
            }
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            if (arch.startsWith("aarch64") || arch.startsWith("arm64")) "skija-macos-arm64" else "skija-macos-x64"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64") && !arch.startsWith("aarch64") && !arch.startsWith("arm")) {
                "skija-windows-x64"
            } else {
                error("Unsupported Skija Windows architecture: $arch")
            }
        else -> error("Unrecognized or unsupported platform. Please set Skija artifact manually.")
    }
}
