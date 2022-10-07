import me.luna.jaroptimizer.JarOptimizerPluginExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import org.spongepowered.asm.gradle.plugins.MixinExtension
import kotlin.math.max

group = "me.luna"
version = "0.0.5"

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }

    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.+")
        classpath("org.spongepowered:mixingradle:0.7.+")
    }
}

plugins {
    idea
    java
    kotlin("jvm")
    id("me.luna.jaroptimizer").version("1.1")
}

apply {
    plugin("net.minecraftforge.gradle")
    plugin("org.spongepowered.mixin")
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://impactdevelopment.github.io/maven/")
}

val library by configurations.creating

val kotlinxCoroutineVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val mappingsChannel: String by project
val mappingsVersion: String by project

dependencies {
    // Jar packaging
    fun ModuleDependency.exclude(moduleName: String): ModuleDependency {
        return exclude(mapOf("module" to moduleName))
    }

    fun jarOnly(dependencyNotation: String) {
        library(dependencyNotation)
    }

    fun implementationAndLibrary(dependencyNotation: String) {
        implementation(dependencyNotation)
        library(dependencyNotation)
    }

    fun implementationAndLibrary(dependencyNotation: String, dependencyConfiguration: ExternalModuleDependency.() -> Unit) {
        implementation(dependencyNotation, dependencyConfiguration)
        library(dependencyNotation, dependencyConfiguration)
    }

    // Forge
    val minecraft = "minecraft"
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    // Dependencies
    implementationAndLibrary("org.jetbrains.kotlin:kotlin-stdlib")
    implementationAndLibrary("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    implementationAndLibrary("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementationAndLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")

    implementationAndLibrary("org.spongepowered:mixin:0.7.+") {
        exclude("commons-io")
        exclude("gson")
        exclude("guava")
        exclude("launchwrapper")
        exclude(group = "org.apache.logging.log4j")
    }

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor") {
        exclude("gson")
    }

    implementationAndLibrary("org.joml:joml:1.10.4")

    implementation("com.github.cabaletta:baritone:1.2.14")
    jarOnly("cabaletta:baritone-api:1.2")
}

idea {
    module {
        excludeDirs.add(file("log.txt"))
        excludeDirs.add(file("run"))
    }
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.troll.refmap.json")
}

configure<UserDevExtension> {
    mappings(mappingsChannel, mappingsVersion)

    runs {
        create("client") {
            workingDirectory = project.file("run").path
            ideaModule("${rootProject.name}.${project.name}.main")

            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
                    "forge.logging.console.level" to "info",
                    "fml.coreMods.load" to "me.luna.trollhack.TrollHackCoreMod",
                    "mixin.env.disableRefMap" to "true"
                )
            )
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    kotlinDaemonJvmArgs = listOf(
        "-Xms1G",
        "-Xmx2G",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+AlwaysPreTouch",
        "-XX:+ParallelRefProcEnabled",
        "-XX:+UseG1GC",
        "-XX:+UseStringDeduplication",
        "-XX:MaxGCPauseMillis=200",
        "-XX:G1NewSizePercent=10",
        "-XX:G1MaxNewSizePercent=25",
        "-XX:G1HeapRegionSize=1M",
        "-XX:G1ReservePercent=10",
        "-XX:G1HeapWastePercent=10",
        "-XX:G1MixedGCCountTarget=8",
        "-XX:InitiatingHeapOccupancyPercent=75",
        "-XX:G1MixedGCLiveThresholdPercent=60",
        "-XX:G1RSetUpdatingPauseTimePercent=30",
        "-XX:G1OldCSetRegionThresholdPercent=25",
        "-XX:SurvivorRatio=8"
    )
}

tasks {
    clean {
        val set = mutableSetOf<Any>()
        buildDir.listFiles()?.filterNotTo(set) {
            it.name == "fg_cache"
        }
        delete = set
    }

    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-Xlambdas=indy",
                "-Xjvm-default=all"
            )
        }
    }

    jar {
        exclude {
            it.name.contains("devfix", true)
        }
    }

    val releaseJar = register<Jar>("releaseJar") {
        finalizedBy(optimizeJars)

        group = "build"
        dependsOn("reobfJar")

        manifest {
            attributes(
                "Manifest-Version" to 1.0,
                "MixinConfigs" to "mixins.troll.core.json, mixins.troll.accessor.json, mixins.troll.patch.json",
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "FMLCorePluginContainsFMLMod" to true,
                "FMLCorePlugin" to "me.luna.trollhack.TrollHackCoreMod",
                "ForceLoadAsMod" to true
            )
        }

        val excludeDirs = listOf("META-INF/com.android.tools", "META-INF/maven", "META-INF/proguard", "META-INF/versions")
        val excludeNames = hashSetOf("module-info", "MUMFREY", "LICENSE", "kotlinx_coroutines_core")

        archiveClassifier.set("Release")

        from(
            jar.get().outputs.files.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        exclude { file ->
            file.name.endsWith("kotlin_module")
                || excludeNames.contains(file.file.nameWithoutExtension)
                || excludeDirs.any { file.path.contains(it) }
        }

        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }

    configure<JarOptimizerPluginExtension> {
        add(releaseJar, "me.luna", "baritone", "org.spongepowered")
    }

    register<Task>("genRuns") {
        group = "ide"
        doLast {
            val threads = Runtime.getRuntime().availableProcessors()
            val vmOptions = listOf(
                "-Xms2G",
                "-Xmx2G",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+AlwaysPreTouch",
                "-XX:+ExplicitGCInvokesConcurrent",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+UseG1GC",
                "-XX:+UseStringDeduplication",
                "-XX:MaxGCPauseMillis=1",
                "-XX:G1NewSizePercent=2",
                "-XX:G1MaxNewSizePercent=10",
                "-XX:G1HeapRegionSize=1M",
                "-XX:G1ReservePercent=15",
                "-XX:G1HeapWastePercent=10",
                "-XX:G1MixedGCCountTarget=16",
                "-XX:InitiatingHeapOccupancyPercent=50",
                "-XX:G1MixedGCLiveThresholdPercent=50",
                "-XX:G1RSetUpdatingPauseTimePercent=25",
                "-XX:G1OldCSetRegionThresholdPercent=5",
                "-XX:SurvivorRatio=5",
                "-XX:ParallelGCThreads=$threads",
                "-XX:ConcGCThreads=${max(threads / 4, 1)}",
                "-XX:FlightRecorderOptions=stackdepth=512",
                "-Dforge.logging.console.level=debug",
                "-Dforge.logging.markers=SCAN,REGISTRIES,REGISTRYDUMP",
                "-Dmixin.env.disableRefMap=true",
                "-Dfml.coreMods.load=me.luna.trollhack.TrollHackCoreMod"
            ).joinToString(" ")

            val dir = File(rootDir, ".idea/runConfigurations")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File(dir, "runClient.xml").writer().use {
                it.write(
                    """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="runClient" type="Application" factoryName="Application">
                            <envs>
                              <env name="MCP_TO_SRG" value="${'$'}PROJECT_DIR$/build/createSrgToMcp/output.srg" />
                              <env name="MOD_CLASSES" value="${'$'}PROJECT_DIR$/build/resources/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/java/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/kotlin/main" />
                              <env name="mainClass" value="net.minecraft.launchwrapper.Launch" />
                              <env name="MCP_MAPPINGS" value="${mappingsChannel}_$mappingsVersion" />
                              <env name="FORGE_VERSION" value="$forgeVersion" />
                              <env name="assetIndex" value="${minecraftVersion.substringBeforeLast('.')}" />
                              <env name="assetDirectory" value="${gradle.gradleUserHomeDir.path.replace('\\', '/')}/caches/forge_gradle/assets" />
                              <env name="nativesDirectory" value="${'$'}PROJECT_DIR$/../${rootProject.name}/build/natives" />
                              <env name="FORGE_GROUP" value="net.minecraftforge" />
                              <env name="tweakClass" value="net.minecraftforge.fml.common.launcher.FMLTweaker" />
                              <env name="MC_VERSION" value="${'$'}{MC_VERSION}" />
                            </envs>
                            <option name="MAIN_CLASS_NAME" value="net.minecraftforge.legacydev.MainClient" />
                            <module name="${rootProject.name}.main" />
                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720 --username TEST" />
                            <option name="VM_PARAMETERS" value="$vmOptions" />
                            <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR$/run" />
                            <method v="2">
                              <option name="Gradle.BeforeRunTask" enabled="true" tasks="prepareRunClient" externalProjectPath="${'$'}PROJECT_DIR$" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                )
            }
        }
    }

    register<Copy>("updateMods") {
        group = "build"
        dependsOn(releaseJar)
        from(releaseJar.get().outputs.files.first())
        into(File(System.getProperty("user.home"), "AppData/Roaming/.minecraft/mods/1.12.2"))
    }

    artifacts {
        archives(releaseJar)
    }
}