package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.*
import org.lwjgl.opengl.GL45.*

class FixedFramebuffer(
    override val width: Int,
    override val height: Int,
    override val useRBO: Boolean,
    msaaLevel: Int,
    override val clearColor: ColorRGBA = ColorRGBA(0f, 0f, 0f, 0f)
) : Framebuffer {
    override var msaaLevel = -1
        set(value) {
            colorAttachments.forEach { it.msaaLevel = value }
            if (value != field) {
                if (value >= 0) {
                    if (useRBO) {
                        if (value == 0) {
                            glNamedRenderbufferStorage(rbo, GL_DEPTH24_STENCIL8, width, height)
                        } else {
                            glNamedRenderbufferStorageMultisample(rbo, value, GL_DEPTH24_STENCIL8, width, height);
                        }
                    }
                }
            }
            field = value
        }
    private var index = 0
    override val fbo: Int = glCreateFramebuffers()
    override val colorAttachments = mutableListOf<Framebuffer.ColorLayer>()
    override val rbo: Int

    fun generateColorLayer(): FixedColorLayer {
        return FixedColorLayer(index++, this, msaaLevel)
    }

    init {
        // rbo
        if (useRBO) {
            rbo = glCreateRenderbuffers()
            this.msaaLevel = msaaLevel
            glNamedFramebufferRenderbuffer(
                fbo,
                GL_DEPTH_STENCIL_ATTACHMENT,
                GL_RENDERBUFFER,
                rbo
            )
        } else rbo = -1
        GL11.glClearColor(clearColor.rFloat, clearColor.gFloat, clearColor.bFloat, clearColor.aFloat)
    }

    class FixedColorLayer(
        override val index: Int,
        override val framebuffer: Framebuffer,
        msaaLevel: Int,
        override val texture: FramebufferTexture = FramebufferTexture(framebuffer.width, framebuffer.height, msaaLevel)
    ) : Framebuffer.ColorLayer, Texture by texture {
        override var msaaLevel = -1
            set(value) {
                if (value >= 0 && value != field) {
                    if (value != 0) {
                        texture.deleteTexture()
                        texture.id = glCreateTextures(GL_TEXTURE_2D_MULTISAMPLE)
                        glTextureStorage2DMultisample(
                            texture.id, value, GL_RGBA8, framebuffer.width, framebuffer.height, true)
                        glNamedFramebufferTexture(framebuffer.fbo, GL_COLOR_ATTACHMENT0 + index, texture.id, 0)
                    } else {
                        texture.deleteTexture()
                        texture.id = glCreateTextures(GL_TEXTURE_2D)
                        glTextureStorage2D(texture.id, 1, GL_RGBA8, framebuffer.width, framebuffer.height)
                        glTextureParameteri(texture.id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                        glTextureParameteri(texture.id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                        glTextureParameteri(texture.id, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                        glTextureParameteri(texture.id, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                        glNamedFramebufferTexture(framebuffer.fbo, GL_COLOR_ATTACHMENT0 + index, texture.id, 0)
                    }
                }
                texture.msaaLevel = value
                field = value
            }

        init {
            this.msaaLevel = msaaLevel
            framebuffer.colorAttachments.add(this)
        }
    }
}