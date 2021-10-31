import net.minecraftforge.gradle.userdev.UserDevExtension
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.spongepowered.asm.gradle.plugins.MixinExtension

group = "cum.xiaro"
version = "0.0.1"

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }

    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:4.+")
        classpath("org.spongepowered:mixingradle:0.7-SNAPSHOT")
    }
}

plugins {
    idea
    java
    kotlin("jvm")
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

    fun jarOnly(dependencyNotation: Any) {
        library(dependencyNotation)
    }

    // Forge
    val minecraft = "minecraft"
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    // Dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")

    implementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        exclude("commons-io")
        exclude("gson")
        exclude("guava")
        exclude("launchwrapper")
        exclude("log4j-core")
    }

    annotationProcessor("org.spongepowered:mixin:0.8.2:processor") {
        exclude("gson")
    }

    implementation("org.joml:joml:1.10.2")

    implementation("com.github.cabaletta:baritone:1.2.14")
    jarOnly("cabaletta:baritone-api:1.2")
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.troll.refmap.json")
}

configure<UserDevExtension> {
    mappings(
        mapOf(
            "channel" to mappingsChannel,
            "version" to mappingsVersion
        )
    )

    runs {
        create("client") {
            workingDirectory = project.file("run").path
            ideaModule("${rootProject.name}.${project.name}.main")

            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
                    "forge.logging.console.level" to "info",
                    "fml.coreMods.load" to "cum.xiaro.trollhack.TrollHackCoreMod",
                    "mixin.env.disableRefMap" to "true"
                )
            )
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                "-Xlambdas=indy"
            )
        }
    }

    jar {
        manifest {
            attributes(
                "Manifest-Version" to 1.0,
                "MixinConfigs" to "mixins.troll.client.json, mixins.troll.accessor.json, mixins.troll.patch.json",
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "FMLCorePluginContainsFMLMod" to true,
                "FMLCorePlugin" to "cum.xiaro.trollhack.TrollHackCoreMod",
                "ForceLoadAsMod" to true
            )
        }

        val regex = "baritone-1\\.2\\.\\d\\d\\.jar".toRegex()
        from(
            (configurations.runtimeClasspath.get() - configurations["minecraft"])
                .filterNot {
                    it.name.matches(regex)
                }.map {
                    if (it.isDirectory) it else zipTree(it)
                }
        )

        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }

    register<Task>("genRuns") {
        group = "ide"
        doLast {
            file(File(rootDir, ".idea/runConfigurations/${project.name}_runClient.xml")).writer().use {
                it.write(
                    """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="${project.name} runClient" type="Application" factoryName="Application">
                            <envs>
                              <env name="MCP_TO_SRG" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/createSrgToMcp/output.srg" />
                              <env name="MOD_CLASSES" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/resources/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/java/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/kotlin/main" />
                              <env name="mainClass" value="net.minecraft.launchwrapper.Launch" />
                              <env name="MCP_MAPPINGS" value="${mappingsChannel}_$mappingsVersion" />
                              <env name="FORGE_VERSION" value="$forgeVersion" />
                              <env name="assetIndex" value="${minecraftVersion.substringBeforeLast('.')}" />
                              <env name="assetDirectory" value="${gradle.gradleUserHomeDir.path.replace('\\', '/')}/caches/forge_gradle/assets" />
                              <env name="nativesDirectory" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/natives" />
                              <env name="FORGE_GROUP" value="net.minecraftforge" />
                              <env name="tweakClass" value="net.minecraftforge.fml.common.launcher.FMLTweaker" />
                              <env name="MC_VERSION" value="${'$'}{MC_VERSION}" />
                            </envs>
                            <option name="MAIN_CLASS_NAME" value="net.minecraftforge.legacydev.MainClient" />
                            <module name="${rootProject.name}.${project.name}.main" />
                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720" />
                            <option name="VM_PARAMETERS" value="-Dforge.logging.console.level=info -Dforge.logging.markers=SCAN,REGISTRIES,REGISTRYDUMP -Dmixin.env.disableRefMap=true -Dfml.coreMods.load=cum.xiaro.trollhack.TrollHackCoreMod" />
                            <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR$/${project.name}/run" />
                            <method v="2">
                              <option name="Gradle.BeforeRunTask" enabled="true" tasks="${project.name}:prepareRunClient" externalProjectPath="${'$'}PROJECT_DIR$" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                )
            }
        }
    }
}