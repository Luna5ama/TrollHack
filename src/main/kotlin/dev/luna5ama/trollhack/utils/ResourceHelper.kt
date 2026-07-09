package dev.luna5ama.trollhack.utils

import com.sun.jna.Native
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object ResourceHelper {

    private val paths = mutableListOf("")

    fun getResourceStream(path: String): InputStream? {
        val path1 = if (path.startsWith("/")) path else "/$path"
        val inJar = ResourceHelper::class.java.getResource(path1)
        if (inJar != null) return inJar.openStream()
        for (p in paths) {
            val name = p + path.removePrefix("/")
            val file = File(name)
            if (file.exists()) {
                return FileInputStream(file)
            }
        }
        return null
    }

    fun extractFromResourcePath(path: String): File? {
        return if (getResourceStream(path)?.close() != null)
            Native.extractFromResourcePath(path)
        else null
    }

    fun addPath(path: String): Boolean {
        val correctedPath = if (path.endsWith("/")) path else "$path/"
        return if (!paths.contains(correctedPath)) {
            paths.add(correctedPath)
            true
        } else false
    }

    fun removePath(path: String): Boolean {
        val correctedPath = if (path.endsWith("/")) path else "$path/"
        return if (paths.contains(correctedPath)) {
            paths.remove(correctedPath)
            true
        } else false
    }

}