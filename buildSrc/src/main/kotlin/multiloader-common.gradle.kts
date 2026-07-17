import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    `java-library`
    `maven-publish`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun catalogVersion(alias: String) = libs.findVersion(alias).get().requiredVersion

val modId = project.property("mod_id").toString()
val modName = project.property("mod_name").toString()
val modAuthor = project.property("mod_author").toString()
val minecraftVersion = catalogVersion("minecraft")
val minecraftVersionRange = catalogVersion("minecraft-range")
val fabricVersion = catalogVersion("fabric-api")
val fabricLoaderVersion = catalogVersion("fabric-loader")
val fabricKotlinVersion = catalogVersion("fabric-kotlin")
val neoForgeVersion = catalogVersion("neoforge")
val neoForgeLoaderVersionRange = catalogVersion("neoforge-loader-range")
val kotlinForForgeVersion = catalogVersion("kotlinforge")
val javaVersion = catalogVersion("java")
val jvmTargetVersion = catalogVersion("jvm-target")

base {
    archivesName.set("${modId}-${project.name}-${minecraftVersion}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/google") {
        content {
            includeGroupByRegex("androidx\\..*")
            includeGroupByRegex("com\\.android(\\..*)?")
        }
    }
    google {
        content {
            includeGroupByRegex("androidx\\..*")
            includeGroupByRegex("com\\.android(\\..*)?")
        }
    }
    maven("https://maven.fabricmc.net")
    maven("https://maven.neoforged.net/releases")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(jvmTargetVersion.toInt())
}

sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { configurationName ->
        configurations.named(configurationName) {
            attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaVersion.toInt())
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "fabric_version" to fabricVersion,
        "fabric_loader_version" to fabricLoaderVersion,
        "fabric_kotlin_version" to fabricKotlinVersion,
        "neoforge_version" to neoForgeVersion,
        "neoforge_loader_version_range" to neoForgeLoaderVersionRange,
        "kotlin_for_forge_version" to kotlinForForgeVersion,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "license" to project.property("license"),
        "description" to project.property("description"),
        "java_version" to javaVersion
    )
    val jsonProps = props.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(props)
    }
    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonProps)
    }
    inputs.properties(props)
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", "LICENSE_${modName}")
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

tasks.withType<Javadoc>().configureEach {
    enabled = false
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
