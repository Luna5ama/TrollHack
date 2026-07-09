package dev.luna5ama.trollhack.config.filesystem

import java.net.URI

interface Path {
    val parent: Path
    val name: String

    fun resolve(child: String): Path

    fun toURI(): URI

    override fun toString(): String

    override fun equals(other: Any?): Boolean

    private class PathImpl(content: String) : Path {
        val content = content.replace("\\", "/")
            get() = field.removeSuffix("/")

        override val parent: Path
            get() = if ("/" in content) PathImpl(content.substring(0, content.lastIndexOf('/'))) else PathImpl("")
        override val name: String
            get() = content.substringAfterLast("/")

        override fun resolve(child: String): Path {
            val relative = content == "." || content == "./"
            if (!relative) return PathImpl(content + "/" + child.removePrefix("./").removePrefix("/"))

            return PathImpl(child.removePrefix("./"))
        }

        override fun toURI(): URI {
            return URI(content)
        }

        override fun toString(): String {
            return content
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PathImpl) return false

            return hashCode() == other.hashCode()
        }

        override fun hashCode(): Int {
            var result = content.hashCode()
            return result
        }
    }

    companion object {
        operator fun invoke(path: String): Path = PathImpl(path)

        operator fun invoke(path: String, vararg subpaths: String): Path {
            var p: Path = PathImpl(path)
            for (path in subpaths) p = p.resolve(path)
            return p
        }
    }
}
