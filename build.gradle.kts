import com.google.gson.JsonParser
import dev.fastmc.loader.ModPackagingTask
import dev.fastmc.loader.ModPlatform
import dev.fastmc.remapper.mapping.MappingName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.math.max

val modGroup: String by project
val modID: String by project
val modName: String by project
var modVersion = project.property("modVersion") as String
System.getenv("OVERRIDE_VERSION")?.let {
    modVersion = it
}

allprojects {
    group = modGroup
    version = modVersion

    repositories {
        mavenCentral()
        maven("https://maven.luna5ama.dev/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://jitpack.io/")
        maven("https://impactdevelopment.github.io/maven/")
    }
}

plugins {
    idea
    java
    kotlin("jvm")

    id("net.minecraftforge.gradle")

    id("com.google.devtools.ksp")
    id("dev.luna5ama.kmogus-struct-plugin") apply false

    id("dev.fastmc.fast-remapper")
    id("dev.luna5ama.jar-optimizer")
    id("dev.fastmc.mod-loader-plugin")
}

allprojects {
    apply {
        plugin("kotlin")
    }
}
val jarLibImplementation: Configuration by configurations.creating

configurations.implementation {
    extendsFrom(jarLibImplementation)
}

val jarLib: Configuration by configurations.creating {
    extendsFrom(jarLibImplementation)
}

afterEvaluate {
    configurations["minecraft"].resolvedConfiguration.resolvedArtifacts.forEach {
        val id = it.moduleVersion.id
        jarLib.exclude(id.group, it.name)
    }
}

val kotlinxCoroutineVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val mappingsChannel: String by project
val mappingsVersion: String by project
val kmogusVersion: String by project

dependencies {
    // Forge
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    // Dependencies
    jarLibImplementation("org.jetbrains.kotlin:kotlin-stdlib")
    jarLibImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    jarLibImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    jarLibImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")

    jarLibImplementation("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    jarLibImplementation("org.joml:joml:1.10.5")
    jarLibImplementation("dev.fastmc:fastmc-common:1.1-SNAPSHOT:java8")

    jarLibImplementation("dev.luna5ama:kmogus-core:$kmogusVersion")
    jarLibImplementation("dev.luna5ama:kmogus-struct-api:$kmogusVersion")
    jarLibImplementation(project(":structs"))

    ksp(project(":codegen"))

    implementation("cabaletta:baritone-deobf-unoptimized-mcp-dev:1.2")
    jarLib("cabaletta:baritone-api:1.2")
}

idea {
    module {
        excludeDirs.add(file("log.txt"))
        excludeDirs.add(file("run"))
    }
}

ksp {
    arg("GROUP", modGroup)
    arg("ID", modID)
    arg("NAME", modName)
    arg("VERSION", modVersion)
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

allprojects {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    kotlin {
        val jvmArgs = mutableSetOf<String>()
        (rootProject.findProperty("kotlin.daemon.jvm.options") as? String)
            ?.split("\\s+".toRegex())?.toCollection(jvmArgs)
        System.getProperty("gradle.kotlin.daemon.jvm.options")
            ?.split("\\s+".toRegex())?.toCollection(jvmArgs)

        kotlinDaemonJvmArgs = jvmArgs.toList()
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
                    "-Xlambdas=indy",
                    "-Xjvm-default=all",
                    "-Xbackend-threads=0"
                )
            }
        }
    }
}

fastRemapper {
    mcVersion(minecraftVersion)
    forge()
    mapping.set(MappingName.Mcp(mappingsChannel, 39))
    minecraftJar.set {
        configurations["minecraft"].copy().apply { isTransitive = false }.singleFile
    }

    mixin(
        "mixins.troll.accessor.json",
        "mixins.troll.core.json",
        "mixins.troll.patch.json"
    )
}

modLoader {
    modPackage.set("dev.luna5ama.loader")
    defaultPlatform.set(ModPlatform.FORGE)
    forgeModClass.set("dev.luna5ama.trollhack.TrollHackMod")
}

tasks {
    processResources {
        outputs.upToDateWhen {
            runCatching {
                val mcmodInfo = outputs.previousOutputFiles.find { it.name == "mcmod.info" }!!
                val obj = JsonParser.parseString(mcmodInfo.readText()).asJsonArray[0].asJsonObject
                obj["modid"].asString == modID
                    && obj["name"].asString == modName
                    && obj["version"].asString == modVersion
            }.getOrDefault(false)
        }

        filesMatching("mcmod.info") {
            expand(
                "MOD_ID" to modID,
                "MOD_NAME" to modName,
                "MOD_VERSION" to modVersion
            )
        }
    }

    register<Task>("releaseBuild") {
        group = "build"
        dependsOn("clean")
        finalizedBy("build")

        doFirst {
            named<ModPackagingTask>("modPackaging") {
                dictSize.set(32 * 1024 * 1024)
            }
        }
    }

    afterEvaluate {
        getByName("reobfJar").enabled = false
    }

    clean {
        val set = mutableSetOf<Any>()
        buildDir.listFiles()?.filterNotTo(set) {
            it.name == "fg_cache"
        }
        delete = set
    }

    jar {
        exclude {
            it.name.contains("devfix", true)
        }

        archiveClassifier.set("devmod")
    }

    modLoaderJar {
        archiveClassifier.set("")
    }
}

val fastRemapJar = fastRemapper.register(tasks.jar)

val fatjar = tasks.register<Jar>("fatjar") {
    val fastRemapJarZipTree = fastRemapJar.map { zipTree(it.archiveFile) }

    dependsOn(fastRemapJar)

    manifest {
        from(fastRemapJarZipTree.map { zipTree -> zipTree.find { it.name == "MANIFEST.MF" }!! })
        attributes(
            "Manifest-Version" to 1.0,
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "FMLCorePlugin" to "dev.luna5ama.trollhack.TrollHackCoreMod",
            "FMLCorePluginContainsFMLMod" to true,
            "ForceLoadAsMod" to true
        )
    }

    val excludeDirs = listOf("META-INF/com.android.tools", "META-INF/maven", "META-INF/proguard", "META-INF/versions")
    val excludeNames = hashSetOf("module-info", "MUMFREY", "LICENSE", "kotlinx_coroutines_core")

    archiveClassifier.set("fatjar")

    from(fastRemapJarZipTree)

    exclude { file ->
        file.name.endsWith("kotlin_module")
            || excludeNames.contains(file.file.nameWithoutExtension)
            || excludeDirs.any { file.path.contains(it) }
    }

    from(
        jarLib.elements.map { set ->
            set.map { fileSystemLocation ->
                fileSystemLocation.asFile.let {
                    if (it.isDirectory) it else zipTree(it)
                }
            }
        }
    )
}

val optimizeFatJar = jarOptimizer.register(fatjar, "dev.luna5ama", "org.spongepowered", "baritone")

afterEvaluate {
    tasks.register<Copy>("updateMods") {
        group = "build"
        from(optimizeFatJar)
        into(provider { File(System.getenv("mod_dir")) })
    }

    artifacts {
        archives(optimizeFatJar)
        archives(tasks.modLoaderJar)
        add("modLoaderPlatforms", optimizeFatJar)
    }
}

tasks {
    register<Task>("genRuns") {
        dependsOn("prepareRuns")
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
                "-Dfml.coreMods.load=dev.luna5ama.trollhack.TrollHackDevFixCoreMod"
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
                              <env name="assetDirectory" value="${
                        gradle.gradleUserHomeDir.path.replace(
                            '\\',
                            '/'
                        )
                    }/caches/forge_gradle/assets" />
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
                              <option name="Make" enabled="true" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                )
            }
        }
    }
}