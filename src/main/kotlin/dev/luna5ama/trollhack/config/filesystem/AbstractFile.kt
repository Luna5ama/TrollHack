package dev.luna5ama.trollhack.config.filesystem

import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipEntry

abstract class AbstractFile(val fs: FileSystem, val path: Path) {
    val parentPath get() = path.parent
    val parentFile get() = createFile(parentPath)

    abstract val absolutePath: String

    val name get() = path.name
    val extension get() = if ('.' in name) name.substringAfterLast(".") else ""
    val nameWithoutExtension get() = if ('.' in name) name.substringBeforeLast(".") else name

    val isFile: Boolean get() = !isDirectory

    abstract val isDirectory: Boolean

    fun resolve(child: String): AbstractFile {
        return createFile(path.resolve(child))
    }

    fun exists() = fs.exists(path)

    fun create() = fs.create(path)
    fun createNewFile() = create()

    fun listFiles() = fs.list(path).map { createFile(it) }

    fun writeText(text: String, charset: Charset = Charsets.UTF_8) = fs.openFileOutputStream(path).use {
        it.write(text.toByteArray(charset))
    }

    fun readText(charset: Charset = Charsets.UTF_8) = fs.openFileInputStream(path).use {
        it.readBytes().toString(charset)
    }

    abstract fun lastModified(): Long

    abstract fun copyTo(other: AbstractFile)

    fun mkdirs() = fs.mkdirs(path)

    fun delete() = fs.delete(path)
    fun deleteRecursively() = delete()

    protected abstract fun createFile(path: Path): AbstractFile
}

class ZipEntryFileImpl(
    fs: FileSystem,
    private val zipEntry: ZipEntry,
    path: Path
) : AbstractFile(fs, path) {
    override val absolutePath: String
        get() = path.toString()
    override val isDirectory: Boolean
        get() = zipEntry.isDirectory

    override fun lastModified(): Long {
        return zipEntry.lastModifiedTime.toMillis()
    }

    override fun copyTo(other: AbstractFile) {
        val data = fs.openFileInputStream(path).use {
            it.readBytes()
        }
        other.fs.openFileOutputStream(other.path).use {
            it.write(data)
        }
    }

    override fun createFile(path: Path): AbstractFile {
        return ZipEntryFileImpl(fs, ZipEntry(path.toString()), path)
    }
}

class DefaultFileImpl(path: Path) : AbstractFile(DefaultFileSystem, path) {
    private val uri = path.toURI()
    private val fileImpl = File(uri.path)

    override val absolutePath: String
        get() = fileImpl.absolutePath.replace('\\', '/')

    constructor(file: File) : this(Path(file.toRelativeString(File("."))))

    override val isDirectory: Boolean
        get() = fileImpl.isDirectory

    override fun lastModified(): Long {
        return fileImpl.lastModified()
    }

    override fun copyTo(other: AbstractFile) {
        if (other is DefaultFileImpl) {
            fileImpl.copyTo(other.fileImpl)
        } else throw UnsupportedOperationException()
    }

    override fun createFile(path: Path): AbstractFile {
        return DefaultFileImpl(path)
    }
}