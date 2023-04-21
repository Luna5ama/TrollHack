package dev.luna5ama.trollhack.util.graphics.font

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.util.graphics.font.glyph.CharInfo
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import javax.imageio.ImageIO

object GlyphCache {
    private val directory = "${TrollHackMod.DIRECTORY}/glyphs"

    fun delete(font: Font) {
        runCatching {
            val regularFont = font.deriveFont(Font.PLAIN)
            File("$directory/${regularFont.hashCode()}").delete()
        }
    }

    fun put(font: Font, chunk: Int, image: BufferedImage, charInfoArray: Array<CharInfo>) {
        val regularFont = font.deriveFont(Font.PLAIN)
        val dirPath = "$directory/${regularFont.hashCode()}"
        val dir = File(dirPath)
        if (!dir.exists()) dir.mkdirs()

        val path = "$dirPath/${font.style}-${chunk}"
        val imageFile = File("$path.png")
        val infoFile = File("$path.info")

        runCatching {
            if (!imageFile.exists()) imageFile.createNewFile()

            ImageIO.write(image, "png", imageFile)
            saveInfo(infoFile, charInfoArray)
        }.onFailure { e ->
            TrollHackMod.logger.info("Failed saving glyph cache", e)
            runCatching {
                imageFile.delete()
                infoFile.delete()
            }.onFailure {
                TrollHackMod.logger.error("Error saving glyph cache", it)
            }
        }
    }

    fun get(font: Font, chunk: Int): Pair<BufferedImage, Array<CharInfo>>? {
        return runCatching {
            val regularFont = font.deriveFont(Font.PLAIN)
            val path = "$directory/${regularFont.hashCode()}/${font.style}-${chunk}"
            val imageFile = File("$path.png")
            val infoFile = File("$path.info")
            if (!imageFile.exists() || !infoFile.exists()) return null

            val image = ImageIO.read(imageFile)
            val info = loadInfo(infoFile)

            image to info
        }.onFailure {
            TrollHackMod.logger.error("Error reading glyph cache", it)
        }.getOrNull()
    }

    fun loadInfo(file: File): Array<CharInfo> {
        return DataInputStream(file.inputStream().buffered(2048)).use { stream ->
            Array(512) {
                CharInfo(
                    stream.readFloat(),
                    stream.readFloat(),
                    stream.readFloat(),
                    stream.readShort(),
                    stream.readShort(),
                    stream.readShort(),
                    stream.readShort()
                )
            }
        }
    }

    fun saveInfo(file: File, charInfoArray: Array<CharInfo>) {
        DataOutputStream(file.outputStream().buffered(2048)).use { stream ->
            for (charInfo in charInfoArray) {
                stream.writeFloat(charInfo.width)
                stream.writeFloat(charInfo.height)
                stream.writeFloat(charInfo.renderWidth)
                stream.writeShort(charInfo.uv[0].toInt())
                stream.writeShort(charInfo.uv[1].toInt())
                stream.writeShort(charInfo.uv[2].toInt())
                stream.writeShort(charInfo.uv[3].toInt())
            }
        }
    }
}