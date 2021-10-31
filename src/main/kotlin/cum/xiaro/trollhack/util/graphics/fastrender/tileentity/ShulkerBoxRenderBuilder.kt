package cum.xiaro.trollhack.util.graphics.fastrender.tileentity

import cum.xiaro.trollhack.util.graphics.fastrender.model.tileentity.ModelShulkerBox
import cum.xiaro.trollhack.util.interfaces.Helper
import net.minecraft.block.BlockShulkerBox
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.ITextureObject
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class ShulkerBoxRenderBuilder(
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double
) : AbstractTileEntityRenderBuilder<TileEntityShulkerBox>(builtPosX, builtPosY, builtPosZ) {
    override fun add(tileEntity: TileEntityShulkerBox) {
        val pos = tileEntity.pos
        val posX = (pos.x + 0.5 - builtPosX).toFloat()
        val posY = (pos.y - builtPosY).toFloat()
        val posZ = (pos.z + 0.5 - builtPosZ).toFloat()

        @Suppress("UNNECESSARY_SAFE_CALL")
        val enumFacing = tileEntity.world?.let {
            val blockState = it.getBlockState(tileEntity.pos)
            if (blockState.block is BlockShulkerBox) {
                blockState.getValue(BlockShulkerBox.FACING) as EnumFacing
            } else {
                EnumFacing.UP
            }
        } ?: EnumFacing.UP

        floatArrayList.add(posX)
        floatArrayList.add(posY)
        floatArrayList.add(posZ)

        putTileEntityLightMapUV(tileEntity)

        when (enumFacing) {
            EnumFacing.DOWN -> {
                byteArrayList.add(0)
                byteArrayList.add(2)
            }
            EnumFacing.UP -> {
                byteArrayList.add(0)
                byteArrayList.add(0)
            }
            EnumFacing.NORTH -> {
                byteArrayList.add(2)
                byteArrayList.add(-1)
            }
            EnumFacing.SOUTH -> {
                byteArrayList.add(0)
                byteArrayList.add(-1)
            }
            EnumFacing.WEST -> {
                byteArrayList.add(-1)
                byteArrayList.add(1)
            }
            EnumFacing.EAST -> {
                byteArrayList.add(1)
                byteArrayList.add(1)
            }
        }

        byteArrayList.add(tileEntity.color.metadata.toByte())
        byteArrayList.add((tileEntity.getProgress(0.0f) * 255.0f).toInt().toByte())
        byteArrayList.add((tileEntity.getProgress(1.0f) * 255.0f).toInt().toByte())

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
        glVertexAttribIPointer(7, 1, GL_BYTE, 20, 15L) // 1
        glVertexAttribIPointer(8, 1, GL_UNSIGNED_BYTE, 20, 16L) // 1
        glVertexAttribPointer(9, 1, GL_UNSIGNED_BYTE, true, 20, 17L) // 1
        glVertexAttribPointer(10, 1, GL_UNSIGNED_BYTE, true, 20, 18L) // 2

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)
        glVertexAttribDivisor(9, 1)
        glVertexAttribDivisor(10, 1)

        model.attachVBO()

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)
        glEnableVertexAttribArray(9)
        glEnableVertexAttribArray(10)

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
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
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
    ) : AbstractTileEntityRenderBuilder.Renderer(shader, vaoID, vboID, modelSize, size, builtPosX, builtPosY, builtPosZ), Helper {
        override fun preRender() {
            GlStateManager.bindTexture(ShulkerTexture.instance.texture.glTextureId)
        }

        override fun postRender() {
            GlStateManager.bindTexture(0)
        }
    }

    private companion object : Helper {
        val model = ModelShulkerBox().apply { init() }
        val shader = Shader("/assets/trollhack/shaders/tileentity/ShulkerBox.vsh", "/assets/trollhack/shaders/tileentity/Default.fsh")
    }

    private class ShulkerTexture private constructor(
        val hash: Int
    ) : Helper {
        val texture: ITextureObject

        init {
            val images = Array(EnumDyeColor.values().size) {
                val iResource = mc.resourceManager.getResource(textures[it])
                TextureUtil.readBufferedImage(iResource.inputStream)
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

        companion object {
            private val textures: Array<ResourceLocation>

            init {
                val enumDyeColors = EnumDyeColor.values()
                textures = Array(enumDyeColors.size) {
                    val enumDyeColor = enumDyeColors[it]
                    ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.dyeColorName}.png")
                }
            }

            var instance = ShulkerTexture(getTextureHash())
                private set
                get() {
                    val newHash = getTextureHash()
                    if (newHash != field.hash) {
                        glDeleteTextures(field.texture.glTextureId)
                        field = ShulkerTexture(newHash)
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