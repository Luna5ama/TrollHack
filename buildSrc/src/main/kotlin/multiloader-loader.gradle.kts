plugins {
    id("multiloader-common")
}

val loaderAttribute = Attribute.of("dev.luna5ama.trollhack.loader", String::class.java)
val commonJava = rootProject.layout.projectDirectory.dir("src/main/java")
val commonKotlin = rootProject.layout.projectDirectory.dir("src/main/kotlin")
val commonResources = rootProject.layout.projectDirectory.dir("src/main/resources")

dependencies {
    compileOnly(project(":")) {
        attributes { attribute(loaderAttribute, "common") }
    }
}

tasks.named<JavaCompile>("compileJava") {
    source(commonJava)
}

tasks.named<ProcessResources>("processResources") {
    from(commonResources)
}

tasks.named<Jar>("sourcesJar") {
    from(commonJava, commonKotlin, commonResources)
}
