import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.neoforged.moddev)
    alias(libs.plugins.fabric.loom) apply false
    id("multiloader-common")
}

val catalogJavaVersion = libs.versions.java.get()
val catalogMinecraftVersion = libs.versions.minecraft.asProvider().get()
val catalogMinecraftVersionRange = libs.versions.minecraft.range.get()
val catalogFabricApiVersion = libs.versions.fabric.api.get()
val catalogFabricLoaderVersion = libs.versions.fabric.loader.get()
val catalogFabricKotlinVersion = libs.versions.fabric.kotlin.get()
val catalogNeoFormVersion = libs.versions.neoform.get()
val catalogNeoForgeVersion = libs.versions.neoforge.asProvider().get()
val catalogNeoForgeLoaderVersionRange = libs.versions.neoforge.loader.range.get()
val catalogKotlinForForgeVersion = libs.versions.kotlinforge.get()

allprojects {
    group = property("maven_group").toString()
    version = property("mod_version").toString()

    extra["java_version"] = catalogJavaVersion
    extra["minecraft_version"] = catalogMinecraftVersion
    extra["minecraft_version_range"] = catalogMinecraftVersionRange
    extra["fabric_version"] = catalogFabricApiVersion
    extra["fabric_loader_version"] = catalogFabricLoaderVersion
    extra["fabric_kotlin_version"] = catalogFabricKotlinVersion
    extra["neo_form_version"] = catalogNeoFormVersion
    extra["neoforge_version"] = catalogNeoForgeVersion
    extra["neoforge_loader_version_range"] = catalogNeoForgeLoaderVersionRange
    extra["kotlin_for_forge_version"] = catalogKotlinForForgeVersion
}

neoForge {
    neoFormVersion = catalogNeoFormVersion
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformers.from(at.absolutePath)
}

dependencies {
    compileOnly(libs.mixin)
    compileOnly(libs.mixinextras.common)
    annotationProcessor(libs.mixinextras.common)
    compileOnly(libs.asm.tree)

    implementation(libs.coroutines.core)
    implementation(libs.reflections)
    implementation(libs.compose.desktop)
    implementation(currentSkikoRuntime())
}

kotlin {
    jvmToolchain(catalogJavaVersion.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        jvmDefault.set(JvmDefaultMode.ENABLE)
        optIn.addAll("kotlin.RequiresOptIn", "kotlin.contracts.ExperimentalContracts")
    }
}

val loaderAttribute = Attribute.of("dev.luna5ama.trollhack.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.named(variant) { attributes { attribute(loaderAttribute, "common") } }
}

fun currentSkikoRuntime() = when {
    System.getProperty("os.name").startsWith("Windows") -> libs.skiko.runtime.windows.x64
    System.getProperty("os.name").startsWith("Mac") && System.getProperty("os.arch").contains("aarch64") -> libs.skiko.runtime.macos.arm64
    System.getProperty("os.name").startsWith("Mac") -> libs.skiko.runtime.macos.x64
    else -> libs.skiko.runtime.linux.x64
}
