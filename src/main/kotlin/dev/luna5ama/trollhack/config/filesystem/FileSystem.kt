package dev.luna5ama.trollhack.config.filesystem

import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface FileSystem {
    fun exists(path: Path): Boolean

    fun list(path: Path): List<Path>

    fun openFile(path: Path): AbstractFile

    fun openFileInputStream(path: Path): InputStream

    fun openFileOutputStream(path: Path): OutputStream

    fun create(path: Path): Boolean

    fun mkdir(path: Path): Boolean

    fun mkdirs(path: Path): Boolean

    fun delete(path: Path): Boolean
}

class ZipFileSystem(val zipFile: ZipFile) : FileSystem {
    override fun exists(path: Path): Boolean {
        return zipFile.getEntry(path.toString()) != null
    }

    override fun list(path: Path): List<Path> {
        var entry = zipFile.getEntry(path.toString())
        if (entry == null) {
            if (path.toString().endsWith("/")) return emptyList()
            entry = zipFile.getEntry("$path/") ?: return emptyList()
        }

        return if (entry.isDirectory) {
            zipFile.entries.toList().map { Path(it.name) }.filter { it.parent == path }
        } else emptyList()
    }

    override fun openFile(path: Path): AbstractFile {
        val name = path.toString()
        return if (!name.endsWith("/")) ZipEntryFileImpl(this, zipFile.getEntry(name) ?: zipFile.getEntry("$name/"), path)
        else ZipEntryFileImpl(this, zipFile.getEntry(name), path)
    }

    override fun openFileInputStream(path: Path): InputStream {
        var stream = zipFile.getInputStream(zipFile.getEntry(path.toString()))
        if (!path.toString().endsWith("/")) stream = stream ?: zipFile.getInputStream(zipFile.getEntry("$path/"))
        return stream
    }

    override fun openFileOutputStream(path: Path): OutputStream {
        throw UnsupportedOperationException()
    }

    override fun create(path: Path): Boolean {
        throw UnsupportedOperationException()
    }

    override fun mkdir(path: Path): Boolean {
        throw UnsupportedOperationException()
    }

    override fun mkdirs(path: Path): Boolean {
        throw UnsupportedOperationException()
    }

    override fun delete(path: Path): Boolean {
        throw UnsupportedOperationException()
    }
}

object DefaultFileSystem : FileSystem {
    override fun exists(path: Path): Boolean {
        return File(path.toURI().path).exists()
    }

    override fun list(path: Path): List<Path> {
        val file = File(path.toURI().path)
        return if (file.isDirectory) file.listFiles().map { Path(it.toRelativeString(File("."))) }
        else emptyList()
    }

    override fun openFile(path: Path): AbstractFile {
        return DefaultFileImpl(path)
    }

    override fun openFileInputStream(path: Path): InputStream {
        return File(path.toURI().path).inputStream()
    }

    override fun openFileOutputStream(path: Path): OutputStream {
        return File(path.toURI().path).outputStream()
    }

    override fun create(path: Path): Boolean {
        return File(path.toURI().path).createNewFile()
    }

    override fun mkdir(path: Path): Boolean {
        return File(path.toURI().path).mkdir()
    }

    override fun mkdirs(path: Path): Boolean {
        return File(path.toURI().path).mkdirs()
    }

    override fun delete(path: Path): Boolean {
        if (exists(path)) {
            val file = File(path.toURI().path)
            return if (file.isDirectory) file.deleteRecursively()
            else file.delete()
        }

        return false
    }
}