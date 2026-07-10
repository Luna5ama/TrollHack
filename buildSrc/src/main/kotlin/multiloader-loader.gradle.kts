plugins {
    id("multiloader-common")
}

val commonJava by configurations.creating { isCanBeResolved = true }
val commonKotlin by configurations.creating { isCanBeResolved = true }
val commonResources by configurations.creating { isCanBeResolved = true }
val loaderAttribute = Attribute.of("dev.luna5ama.trollhack.loader", String::class.java)

dependencies {
    compileOnly(project(":")) {
        attributes { attribute(loaderAttribute, "common") }
    }
    commonJava(project(path = ":", configuration = "commonJava"))
    commonKotlin(project(path = ":", configuration = "commonKotlin"))
    commonResources(project(path = ":", configuration = "commonResources"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(commonResources)
    from(commonResources)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(commonJava, commonKotlin, commonResources)
    from(commonJava, commonKotlin, commonResources)
}
