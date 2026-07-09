package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.config.filesystem.AbstractFile
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

fun File.recreateAndBackup() {
    if (isDirectory) throw UnsupportedOperationException("Backup directories is not supported")
    if (exists()) {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")
        val formattedDate = formatter.format(Date.from(Instant.now()))
        val target = parentFile.resolve("$name-$formattedDate.bak")
        if (target.exists()) target.delete()
        else copyTo(target)
    } else if (!parentFile.exists()) parentFile.mkdirs()
    createNewFile()
}

fun AbstractFile.recreateAndBackup() {
    if (isDirectory) throw UnsupportedOperationException("Backup directories is not supported")
    if (exists()) {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")
        val formattedDate = formatter.format(Date.from(Instant.now()))
        val target = parentFile.resolve("$name-$formattedDate.bak")
        if (target.exists()) target.delete()
        else copyTo(target)
    } else if (!parentFile.exists()) parentFile.mkdirs()
    createNewFile()
}


fun File.listFilesRecursively(): List<File> {
    return listFiles()?.flatMap {
        if (it.isDirectory) it.listFilesRecursively()
        else listOf(it)
    } ?: emptyList()
}


fun AbstractFile.listFilesRecursively(): List<AbstractFile> {
    return listFiles().flatMap {
        if (it.isDirectory) it.listFilesRecursively()
        else listOf(it)
    }
}