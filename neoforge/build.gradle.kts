import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("multiloader-loader")
    alias(libs.plugins.neoforged.moddev)
}

val modId = project.property("mod_id").toString()
val embedded by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
    exclude(group = "org.jetbrains")
    exclude(group = "org.slf4j")
    exclude(group = "org.jspecify")
    exclude(group = "com.google.code.findbugs")
}
configurations.named("jarJar") {
    dependencies.addAllLater(provider {
        embedded.incoming.artifacts.resolvedArtifacts.get()
            .mapNotNull { it.id.componentIdentifier as? ModuleComponentIdentifier }
            .distinctBy { "${it.group}:${it.module}:${it.version}" }
            .map { id ->
                project.dependencies.create("${id.group}:${id.module}:${id.version}").apply {
                    isTransitive = false
                }
            }
    })
}
dependencies {
    implementation(libs.kotlinforge)

    implementation(libs.reflections)
    implementation(libs.compose.desktop)
    implementation(currentSkikoRuntime())

    embedded(libs.reflections)
    embedded(libs.compose.desktop)
    embedded(libs.skiko.runtime.windows.x64)
    compileOnly(libs.mixinextras.common)
    annotationProcessor(libs.mixinextras.common)
    compileOnly(libs.asm.tree)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        jvmDefault.set(JvmDefaultMode.ENABLE)
        optIn.addAll("kotlin.RequiresOptIn", "kotlin.contracts.ExperimentalContracts")
    }
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("src/main/kotlin"))
    }
}

tasks.named<ProcessResources>("processResources") {
    exclude("$modId.accesswidener")
}

tasks.named<Jar>("sourcesJar") {
    exclude("$modId.accesswidener")
}

neoForge {
    version = project.property("neoforge_version").toString()
    val at = rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformers.from(at.absolutePath)
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})"
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
        }
    }
    mods {
        register(modId) { sourceSet(sourceSets.main.get()) }
    }
}

val loaderAttribute = Attribute.of("dev.luna5ama.trollhack.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.named(variant) { attributes { attribute(loaderAttribute, "neoforge") } }
}

fun currentSkikoRuntime() = when {
    System.getProperty("os.name").startsWith("Windows") -> libs.skiko.runtime.windows.x64
    System.getProperty("os.name").startsWith("Mac") && System.getProperty("os.arch").contains("aarch64") -> libs.skiko.runtime.macos.arm64
    System.getProperty("os.name").startsWith("Mac") -> libs.skiko.runtime.macos.x64
    else -> libs.skiko.runtime.linux.x64
}
