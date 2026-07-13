import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("multiloader-loader")
    alias(libs.plugins.fabric.loom)
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
configurations.named("include") {
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
    minecraft(libs.minecraft)
    mappings(loom.layered { officialMojangMappings() })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

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
    exclude("META-INF/accesstransformer.cfg")
}

tasks.named<Jar>("sourcesJar") {
    exclude("META-INF/accesstransformer.cfg")
}

loom {
    val aw = rootProject.file("src/main/resources/$modId.accesswidener")
    if (aw.exists()) accessWidenerPath.set(aw)
    mixin { defaultRefmapName.set("$modId.refmap.json") }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("runs/client")
        }
    }
}

val loaderAttribute = Attribute.of("dev.luna5ama.trollhack.loader", String::class.java)
configurations.matching {
    it.name in listOf("apiElements", "runtimeElements", "sourcesElements", "includeInternal", "modCompileClasspath")
}.configureEach {
    attributes { attribute(loaderAttribute, "fabric") }
}

fun currentSkikoRuntime() = when {
    System.getProperty("os.name").startsWith("Windows") -> libs.skiko.runtime.windows.x64
    System.getProperty("os.name").startsWith("Mac") && System.getProperty("os.arch").contains("aarch64") -> libs.skiko.runtime.macos.arm64
    System.getProperty("os.name").startsWith("Mac") -> libs.skiko.runtime.macos.x64
    else -> libs.skiko.runtime.linux.x64
}
