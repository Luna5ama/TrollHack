package dev.luna5ama.trollhack.graphics.fastrender.tileentity

import dev.luna5ama.trollhack.graphics.fastrender.model.tileentity.ModelBed
import dev.luna5ama.trollhack.graphics.texture.TextureUtils.readImage
import dev.luna5ama.trollhack.util.interfaces.Helper
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.ITextureObject
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityBed
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.PI

class BedRenderBuilder(
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double
) : AbstractTileEntityRenderBuilder<TileEntityBed>(builtPosX, builtPosY, builtPosZ) {
    override fun add(tileEntity: TileEntityBed) {
        val pos = tileEntity.pos
        val posX = (pos.x + 0.5 - builtPosX).toFloat()
        val posY = (pos.y - builtPosY).toFloat()
        val posZ = (pos.z + 0.5 - builtPosZ).toFloat()

        val isHead = if (tileEntity.hasWorld()) tileEntity.isHeadPiece else true

        floatArrayList.add(posX)
        floatArrayList.add(posY)
        floatArrayList.add(posZ)

        putTileEntityLightMapUV(tileEntity)

        when (getTileEntityBlockMetadata(tileEntity) and 3) {
            1 -> {
                byteArrayList.add(-1)
            }
            2 -> {
                byteArrayList.add(0)
            }
            3 -> {
                byteArrayList.add(1)
            }
            else -> {
                byteArrayList.add(2)
            }
        }

        byteArrayList.add(tileEntity.color.metadata.toByte())
        byteArrayList.add(if (isHead) 1 else 0)

        size++
    }

    override fun uploadBuffer(buffer: ByteBuffer): AbstractTileEntityRenderBuilder.Renderer {
        val vaoID = glGenVertexArrays()
        val vboID = glGenBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(vaoID)

        glVertexAttribPointer(4, 3, GL_FLOAT, false, 20, 0L) // 12
        glVertexAttribPointer(5, 2, GL_UNSIGNED_BYTE, true, 20, 12L) // 2

        glVertexAttribIPointer(6, 1, GL_BYTE, 20, 14L) // 1
        glVertexAttribIPointer(7, 1, GL_UNSIGNED_BYTE, 20, 15L) // 1
        glVertexAttribIPointer(8, 1, GL_UNSIGNED_BYTE, 20, 16L) // 1

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)

        model.attachVBO()

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        return Renderer(shader, vaoID, vboID, model.modelSize, size, builtPosX, builtPosY, builtPosZ)
    }

    override fun buildBuffer(): ByteBuffer {
        val buffer = GLAllocation.createDirectByteBuffer(size * 20)

        var floatIndex = 0
        var byteIndex = 0

        for (i in 0 until size) {
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(0)
            buffer.put(0)
            buffer.put(0)
        }

        buffer.flip()

        return buffer
    }

    private class Renderer(
        shader: Shader,
        vaoID: Int,
        vboID: Int,
        modelSize: Int,
        size: Int,
        builtPosX: Double,
        builtPosY: Double,
        builtPosZ: Double,
    ) : AbstractTileEntityRenderBuilder.Renderer(
        shader,
        vaoID,
        vboID,
        modelSize,
        size,
        builtPosX,
        builtPosY,
        builtPosZ
    ), Helper {
        override fun preRender() {
            GlStateManager.bindTexture(BedTexture.instance.texture.glTextureId)
        }

        override fun postRender() {
            GlStateManager.bindTexture(0)
        }
    }

    private companion object : Helper {
        val model = ModelBed().apply { init() }
        val shader =
            Shader("/assets/trollhack/shaders/tileentity/Bed.vsh", "/assets/trollhack/shaders/tileentity/Default.fsh")
    }

    private class BedTexture private constructor(
        val hash: Int
    ) : Helper {
        val texture: ITextureObject

        init {
            val images = Array(EnumDyeColor.values().size) {
                val iResource = mc.resourceManager.getResource(textures[it])
                val oldImage = iResource.readImage()
                transformBedTexture(oldImage)
            }

            val firstImage = images[0]
            val size = firstImage.width
            val finalImage = BufferedImage(size * 4, size * 4, firstImage.type)
            val graphics = finalImage.createGraphics()

            for (x in 0 until 4) {
                for (y in 0 until 4) {
                    val src = images[x + y * 4]
                    graphics.drawImage(src, x * size, y * size, null)
                }
            }

            graphics.dispose()

            texture = DynamicTexture(finalImage)
        }

        private fun transformBedTexture(oldImage: BufferedImage): BufferedImage {
            val newImage = BufferedImage(oldImage.width, oldImage.height, oldImage.type)
            val scale = oldImage.width / 64

            val graphics = newImage.createGraphics()
            val identity = AffineTransform()

            drawHead(graphics, oldImage, scale, identity)
            drawFoot(graphics, oldImage, scale, identity)

            drawLeg0(graphics, oldImage, scale)
            drawLeg1(graphics, oldImage, scale)
            drawLeg2(graphics, oldImage, scale)
            drawLeg3(graphics, oldImage, scale)

            graphics.dispose()

            return newImage
        }

        private fun drawHead(graphics: Graphics2D, oldImage: BufferedImage, scale: Int, identity: AffineTransform) {
            graphics.rotate(PI / -2.0, 0.0, 22.0)
            graphics.drawImage(
                oldImage,
                0,
                22 * scale,
                6 * scale,
                38 * scale,
                0,
                6 * scale,
                6 * scale,
                22 * scale,
                null
            )

            graphics.transform = identity
            graphics.rotate(-PI, 48.0, 16.0)
            graphics.drawImage(
                oldImage,
                32 * scale,
                10,
                48 * scale,
                16 * scale,
                6 * scale,
                0,
                22 * scale,
                6 * scale,
                null
            )

            graphics.transform = identity
            graphics.rotate(PI / 2.0, 48.0, 22.0)
            graphics.drawImage(
                oldImage,
                42 * scale,
                22 * scale,
                48 * scale,
                38 * scale,
                22 * scale,
                6 * scale,
                28 * scale,
                22 * scale,
                null
            )

            graphics.transform = identity
            graphics.drawImage(
                oldImage,
                16 * scale,
                0,
                32 * scale,
                16 * scale,
                6 * scale,
                6 * scale,
                22 * scale,
                22 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                32 * scale,
                0,
                48 * scale,
                16 * scale,
                28 * scale,
                28 * scale,
                44 * scale,
                44 * scale,
                null
            )
        }

        private fun drawFoot(graphics: Graphics2D, oldImage: BufferedImage, scale: Int, identity: AffineTransform) {
            graphics.rotate(PI / -2.0, 0.0, 44.0)
            graphics.drawImage(
                oldImage,
                0,
                44 * scale,
                6 * scale,
                60,
                0,
                28 * scale,
                6 * scale,
                44 * scale,
                null
            )

            graphics.transform = identity
            graphics.rotate(PI, 16.0, 38.0)
            graphics.drawImage(
                oldImage,
                0,
                32 * scale,
                16 * scale,
                38 * scale,
                22 * scale,
                22 * scale,
                38 * scale,
                28 * scale,
                null
            )

            graphics.transform = identity
            graphics.rotate(PI / 2.0, 48.0, 44.0)
            graphics.drawImage(
                oldImage,
                42 * scale,
                44 * scale,
                48 * scale,
                60,
                22 * scale,
                28 * scale,
                28 * scale,
                44 * scale,
                null
            )

            graphics.transform = identity
            graphics.drawImage(
                oldImage,
                16 * scale,
                22 * scale,
                32 * scale,
                38 * scale,
                6 * scale,
                28 * scale,
                22 * scale,
                44 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                32 * scale,
                22 * scale,
                48 * scale,
                38 * scale,
                28 * scale,
                28 * scale,
                44 * scale,
                44 * scale,
                null
            )
        }

        private fun drawLeg0(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
            graphics.drawImage(
                oldImage,
                3 * scale,
                44 * scale,
                9 * scale,
                47 * scale,
                53 * scale,
                0,
                59 * scale,
                3 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                0,
                47 * scale,
                9 * scale,
                50,
                53 * scale,
                3 * scale,
                62 * scale,
                6 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                9 * scale,
                47 * scale,
                12 * scale,
                50,
                50,
                3 * scale,
                53 * scale,
                6 * scale,
                null
            )
        }

        private fun drawLeg1(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
            graphics.drawImage(
                oldImage,
                3 * scale,
                50,
                9 * scale,
                53 * scale,
                53 * scale,
                0,
                59 * scale,
                3 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                0,
                53 * scale,
                6 * scale,
                56 * scale,
                56 * scale,
                3 * scale,
                62 * scale,
                6 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                6 * scale,
                53 * scale,
                12 * scale,
                56 * scale,
                50,
                3 * scale,
                56 * scale,
                6 * scale,
                null
            )
        }

        private fun drawLeg2(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
            graphics.drawImage(
                oldImage,
                15 * scale,
                44 * scale,
                21 * scale,
                47 * scale,
                53 * scale,
                0,
                59 * scale,
                3 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                12 * scale,
                47 * scale,
                24 * scale,
                50,
                50,
                3 * scale,
                62 * scale,
                6 * scale,
                null
            )
        }

        private fun drawLeg3(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
            graphics.drawImage(
                oldImage,
                15 * scale,
                50,
                21 * scale,
                53 * scale,
                53 * scale,
                0,
                59 * scale,
                3 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                12 * scale,
                53 * scale,
                15 * scale,
                56 * scale,
                59 * scale,
                3 * scale,
                62 * scale,
                6 * scale,
                null
            )

            graphics.drawImage(
                oldImage,
                15 * scale,
                53 * scale,
                24 * scale,
                56 * scale,
                50,
                3 * scale,
                59 * scale,
                6 * scale,
                null
            )
        }

        companion object {
            private val textures: Array<ResourceLocation>

            init {
                val enumDyeColors = EnumDyeColor.values()
                textures = Array(enumDyeColors.size) {
                    val enumDyeColor = enumDyeColors[it]
                    ResourceLocation("textures/entity/bed/${enumDyeColor.dyeColorName}.png")
                }
            }

            var instance = BedTexture(getTextureHash())
                private set
                get() {
                    val newHash = getTextureHash()
                    if (newHash != field.hash) {
                        glDeleteTextures(field.texture.glTextureId)
                        field = BedTexture(newHash)
                    }

                    return field
                }

            private fun getTextureHash(): Int {
                var result = 1

                for (element in textures) {
                    val textureObject = mc.renderEngine.getTexture(element)
                    @Suppress("UNNECESSARY_SAFE_CALL")
                    result = 31 * result + (textureObject?.hashCode() ?: 0)
                }

                return result
            }
        }
    }
}