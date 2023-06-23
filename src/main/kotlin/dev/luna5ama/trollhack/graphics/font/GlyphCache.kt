package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.graphics.font.glyph.CharInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Font
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


object GlyphCache {
    private const val oldDir = "${TrollHackMod.DIRECTORY}/glyphs"
    private const val directory = "${TrollHackMod.DIRECTORY}/glyph_cache_v1"

    init {
        runCatching {
            File(oldDir).deleteRecursively()
        }
    }

    fun delete(font: Font) {
        runCatching {
            val regularFont = font.deriveFont(Font.PLAIN)
            File("$directory/${regularFont.hashName()}").delete()
        }
    }

    suspend fun put(font: Font, chunk: Int, entry: Entry) {
        val regularFont = font.deriveFont(Font.PLAIN)
        val dirPath = "$directory/${regularFont.hashName()}"
        val dir = File(dirPath)
        if (!dir.exists()) dir.mkdirs()

        val path = "$dirPath/${font.style}-${chunk}"
        val infoFile = File("$path.bin.gz")

        runCatching {
            saveInfo(infoFile, entry)
        }.onFailure { e ->
            TrollHackMod.logger.info("Failed saving glyph cache", e)
            runCatching {
                infoFile.delete()
            }.onFailure {
                TrollHackMod.logger.error("Error saving glyph cache", it)
            }
        }
    }

    suspend fun get(font: Font, chunk: Int): Entry? {
        return runCatching {
            val regularFont = font.deriveFont(Font.PLAIN)
            val path = "$directory/${regularFont.hashName()}/${font.style}-${chunk}"
            val infoFile = File("$path.bin.gz")
            if (!infoFile.exists()) return null

            loadInfo(infoFile)
        }.onFailure {
            TrollHackMod.logger.error("Error reading glyph cache", it)
        }.getOrNull()
    }

    private suspend fun loadInfo(file: File): Entry {
        return withContext(Dispatchers.IO) {
            val uncompressedSize = RandomAccessFile(file, "r").use {
                it.seek(it.length() - 4)
                val b4 = it.read()
                val b3 = it.read()
                val b2 = it.read()
                val b1 = it.read()
                (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or (b4)
            }
            DataInputStream(GZIPInputStream(file.inputStream().buffered()).buffered()).use {
                val count = it.readInt()
                val width = it.readInt()
                val height = it.readInt()
                val levels = it.readInt()
                val baseSize = it.readInt()
                val charInfoArray = Array(count) { _ ->
                    CharInfo(
                        it.readFloat(),
                        it.readFloat(),
                        it.readFloat(),
                        shortArrayOf(
                            it.readShort(),
                            it.readShort(),
                            it.readShort(),
                            it.readShort()
                        )
                    )
                }
                val headerSize = 20 + 20 * count
                val data = ByteArray(uncompressedSize - headerSize)
                it.readFully(data)
                Entry(count, width, height, levels, baseSize, charInfoArray, data)
            }
        }

    }

    private suspend fun saveInfo(file: File, entry: Entry) {
        withContext(Dispatchers.IO) {
            DataOutputStream(GZIPOutputStream(file.outputStream()).buffered()).use {
                it.writeInt(entry.count)
                it.writeInt(entry.width)
                it.writeInt(entry.height)
                it.writeInt(entry.levels)
                it.writeInt(entry.baseSize)
                for (charInfo in entry.charInfoArray) {
                    it.writeFloat(charInfo.width)
                    it.writeFloat(charInfo.height)
                    it.writeFloat(charInfo.renderWidth)
                    it.writeShort(charInfo.uv[0].toInt())
                    it.writeShort(charInfo.uv[1].toInt())
                    it.writeShort(charInfo.uv[2].toInt())
                    it.writeShort(charInfo.uv[3].toInt())
                }
                it.write(entry.data)
            }
        }
    }

    private fun Font.hashName(): String {
        val transforms = DoubleArray(6)
        transform.getMatrix(transforms)
        val str = "$name:$size:${transforms.contentToString()}"
        val hash = MessageDigest.getInstance("MD5").digest(str.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    data class Entry(
        val count: Int,
        val width: Int,
        val height: Int,
        val levels: Int,
        val baseSize: Int,
        val charInfoArray: Array<CharInfo>,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false

            if (count != other.count) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (levels != other.levels) return false
            if (baseSize != other.baseSize) return false
            if (!charInfoArray.contentEquals(other.charInfoArray)) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = count
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + levels
            result = 31 * result + baseSize
            result = 31 * result + charInfoArray.contentHashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}