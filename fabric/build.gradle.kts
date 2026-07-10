import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    id("multiloader-loader")
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.shadow)
}

val modId = project.property("mod_id").toString()
val shade by configurations.creating
configurations.named("implementation") { extendsFrom(shade) }

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered { officialMojangMappings() })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    shade(libs.reflections)
    shade(libs.okhttp)
    shade(libs.reflect)
    shade(libs.jcodec)
    shade(libs.jcodec.javase)
    shade(libs.compose.desktop)
    shade(libs.skiko.awt)
    shade(currentSkikoRuntime())
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
        kotlin.srcDir(configurations.named("commonKotlin"))
    }
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

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    archiveClassifier.set("dev-shadow")
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
    }
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
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
