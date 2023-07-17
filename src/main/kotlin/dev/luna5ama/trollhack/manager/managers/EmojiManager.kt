package dev.luna5ama.trollhack.manager.managers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.readText
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object EmojiManager : Manager() {
    private val localFile = File("${TrollHackMod.DIRECTORY}/emoji.zip")
    private const val versionURL = "https://raw.githubusercontent.com/2b2t-Utilities/emojis/master/version.json"
    private const val zipUrl = "https://github.com/2b2t-Utilities/emojis/archive/master.zip"

    private val parser = JsonParser()
    private val emojiMap = HashMap<String, MipmapTexture>()
    private var zipCache: ZipCache? = null

    private val job = DefaultScope.launch(Dispatchers.IO) {
        try {
            checkEmojiUpdate()
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to check emoji update", e)
        }

        try {
            zipCache = ZipCache(ZipFile(localFile))
            TrollHackMod.logger.info("Emoji Initialized")
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to load emojis", e)
        }
    }

    private fun checkEmojiUpdate() {
        val globalVer = streamToJson(URL(versionURL).openStream())
        val localVer = getLocalVersion()

        if (localVer == null
            || globalVer != null && globalVer["version"] != localVer["version"]
        ) {
            URL(zipUrl).openStream().use { online ->
                localFile.outputStream().use {
                    online.copyTo(it, 1024 * 1024)
                }
            }
        }
    }

    private fun getLocalVersion(): JsonObject? {
        if (!localFile.exists()) {
            return null
        }

        val zipFile = ZipFile(localFile)
        return zipFile.entries().asSequence().filterNot {
            it.isDirectory
        }.find {
            it.name.substringAfterLast("/") == "version.json"
        }?.let {
            streamToJson(zipFile.getInputStream(it))
        }
    }

    private fun streamToJson(stream: InputStream): JsonObject? {
        return try {
            stream.use {
                parser.parse(it.readText()).asJsonObject
            }
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed to parse emoji version Json", e)
            null
        }
    }

    fun getEmoji(name: String?): MipmapTexture? {
        if (name == null) return null

        // Returns null if still loading
        if (job.isActive) {
            return null
        }

        // Loads emoji on demand
        if (!emojiMap.containsKey(name)) {
            loadEmoji(name)
        }

        return emojiMap[name]
    }

    fun isEmoji(name: String?) = getEmoji(name) != null

    private fun loadEmoji(name: String) {
        val inputStream = zipCache?.get(name) ?: return

        try {
            val image = inputStream.use {
                ImageIO.read(it)
            }
            val texture = MipmapTexture(image, GL_RGBA, 3)

            texture.bindTexture()
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.5f)
            texture.unbindTexture()

            emojiMap[name] = texture
        } catch (e: IOException) {
            TrollHackMod.logger.warn("Failed to load emoji", e)
        }
    }

    private class ZipCache(private val zipFile: ZipFile) {
        private val entries = zipFile.entries().asSequence().filterNot {
            it.isDirectory
        }.filter {
            it.name.endsWith(".png")
        }.associateBy {
            it.name.substring(it.name.lastIndexOf('/') + 1, it.name.lastIndexOf('.'))
        }

        operator fun get(name: String): InputStream? {
            return entries[name]?.let {
                zipFile.getInputStream(it)
            }
        }
    }
}