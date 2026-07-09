package dev.luna5ama.trollhack.graphics.media

import dev.luna5ama.trollhack.graphics.texture.ImageUtils
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import dev.luna5ama.trollhack.graphics.texture.Texture
import dev.luna5ama.trollhack.utils.timing.NanoTickTimer
import org.jcodec.api.FrameGrab
import org.jcodec.common.io.NIOUtils
import org.jcodec.scale.AWTUtil
import org.lwjgl.opengl.GL11
import java.io.File
import kotlin.math.roundToLong

class SoftVideoDecoder(val file: File) {
    private val grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file))
    private val fps = grab.videoTrack.meta.totalFrames / grab.videoTrack.meta.totalDuration
    private val interval = (1000000000 / fps).roundToLong()
    private val timer = NanoTickTimer()
    private var nativeFrame = grab.nativeFrame
    private val frameTexture = MipmapTexture.Companion.instant(AWTUtil.toBufferedImage(nativeFrame))

    init {
        timer.reset()
    }

    fun render(): Texture {
        timer.tps(fps.toInt()) {
            nativeFrame = grab.nativeFrame ?: return@tps

            ImageUtils.uploadImage(frameTexture, AWTUtil.toBufferedImage(nativeFrame),
                GL11.GL_RGBA, frameTexture.width, frameTexture.height)
        }

        return frameTexture
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SoftVideoDecoder(File("C:\\Users\\xigua\\Videos\\NVIDIA\\Apex Legends\\Apex Legends 2024.11.30 - 21.04.15.01.mp4"))
        }
    }
}