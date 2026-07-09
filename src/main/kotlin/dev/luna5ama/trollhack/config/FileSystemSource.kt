package dev.luna5ama.trollhack.config

import dev.luna5ama.trollhack.config.filesystem.AbstractFile
import dev.luna5ama.trollhack.config.filesystem.DefaultFileSystem
import dev.luna5ama.trollhack.config.filesystem.Path
import dev.luna5ama.trollhack.config.filesystem.ZipFileSystem
import org.apache.commons.compress.archivers.zip.ZipFile

sealed interface FileSystemSource {
    val root: AbstractFile

    data object DefaultDiskSource : FileSystemSource {
        override val root: AbstractFile get() = DefaultFileSystem.openFile(Path("."))
    }

    class ZipFileSource(val zipFile: ZipFile) : FileSystemSource {
        private val fs = ZipFileSystem(zipFile)
        override val root: AbstractFile
            get() = fs.openFile(Path("."))
    }
}