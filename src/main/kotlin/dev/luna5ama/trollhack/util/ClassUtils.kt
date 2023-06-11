package dev.luna5ama.trollhack.util

import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.util.function.Predicate
import java.util.jar.JarInputStream

object ClassUtils {
    fun findClasses(
        packageName: String,
        predicate: Predicate<String> = Predicate { true }
    ): List<Class<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')

        val root = if (packageName.startsWith("dev.luna5ama.trollhack")) {
            val thisFileName = this.javaClass.name.replace('.', '/') + ".class"
            val thisURL = classLoader.getResource(thisFileName)!!
            val file = thisURL.file.substringBeforeLast(thisFileName.substringAfter("dev/luna5ama/trollhack"))
            URL(thisURL.protocol, thisURL.host, file)
        } else {
            classLoader.getResource(packagePath)!!
        }

        val isJar = root.toString().startsWith("jar")
        val classes = ArrayList<Class<*>>()

        if (isJar) {
            val path = Paths.get(URL(root.path.substringBeforeLast('!')).toURI())
            findClassesInJar(classLoader, path.toFile(), packagePath, predicate, classes)
        } else {
            classLoader.getResources(packagePath)?.let {
                for (url in it) {
                    findClasses(classLoader, File(url.file), packageName, predicate, classes)
                }
            }
        }

        return classes
    }

    private fun findClasses(
        classLoader: ClassLoader,
        directory: File,
        packageName: String,
        predicate: Predicate<String> = Predicate { true },
        list: MutableList<Class<*>>
    ): List<Class<*>> {
        if (!directory.exists()) return list

        val packagePath = packageName.replace('.', File.separatorChar)

        directory.walk()
            .filter { it.isFile }
            .filter { it.extension == "class" }
            .map { it.path.substringAfter(packagePath) }
            .map { it.replace(File.separatorChar, '.') }
            .map { it.substring(0, it.length - 6) }
            .map { "$packageName$it" }
            .filter { predicate.test(it) }
            .map { Class.forName(it, false, classLoader) }
            .toCollection(list)

        return list
    }

    private fun findClassesInJar(
        classLoader: ClassLoader,
        jarFile: File,
        packageName: String,
        predicate: Predicate<String> = Predicate { true },
        list: MutableList<Class<*>>
    ) {
        JarInputStream(jarFile.inputStream().buffered(1024 * 1024)).use {
            var entry = it.nextJarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name

                    if (name.startsWith(packageName) && name.endsWith(".class")) {
                        val className = name
                            .replace('/', '.')
                            .substring(0, name.length - 6)

                        if (predicate.test(className)) {
                            list.add(Class.forName(className, false, classLoader))
                        }
                    }
                }

                entry = it.nextJarEntry
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val <T> Class<out T>.instance
        get() = this.getDeclaredField("INSTANCE")[null] as T
}