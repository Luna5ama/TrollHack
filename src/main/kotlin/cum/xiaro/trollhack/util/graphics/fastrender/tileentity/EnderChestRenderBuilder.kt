package cum.xiaro.trollhack.util.graphics.fastrender.tileentity

import cum.xiaro.trollhack.util.graphics.fastrender.model.tileentity.ModelChest
import cum.xiaro.trollhack.util.interfaces.Helper
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.TileEntityEnderChest
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.nio.ByteBuffer

class EnderChestRenderBuilder(
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double
) : AbstractTileEntityRenderBuilder<TileEntityEnderChest>(builtPosX, builtPosY, builtPosZ) {
    override fun add(tileEntity: TileEntityEnderChest) {
        val pos = tileEntity.pos
        val posX = (pos.x + 0.5 - builtPosX).toFloat()
        val posY = (pos.y - builtPosY).toFloat()
        val posZ = (pos.z + 0.5 - builtPosZ).toFloat()

        floatArrayList.add(posX)
        floatArrayList.add(posY)
        floatArrayList.add(posZ)

        putTileEntityLightMapUV(tileEntity)

        when (getTileEntityBlockMetadata(tileEntity)) {
            2 -> {
                byteArrayList.add(2)
            }
            4 -> {
                byteArrayList.add(-1)
            }
            5 -> {
                byteArrayList.add(1)
            }
            else -> {
                byteArrayList.add(0)
            }
        }

        shortArrayList.add((tileEntity.prevLidAngle * 65535.0f).toInt().toShort())
        shortArrayList.add((tileEntity.lidAngle * 65535.0f).toInt().toShort())

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
        glVertexAttribPointer(7, 1, GL_UNSIGNED_SHORT, true, 20, 15L) // 2
        glVertexAttribPointer(8, 1, GL_UNSIGNED_SHORT, true, 20, 17L) // 2

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
        var shortIndex = 0
        var byteIndex = 0

        for (i in 0 until size) {
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.putFloat(floatArrayList.getFloat(floatIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.put(byteArrayList.getByte(byteIndex++))
            buffer.putShort(shortArrayList.getShort(shortIndex++))
            buffer.putShort(shortArrayList.getShort(shortIndex++))
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
            mc.renderEngine.bindTexture(texture)
        }

        override fun postRender() {
            GlStateManager.bindTexture(0)
        }
    }

    private companion object {
        val model = ModelChest().apply { init() }
        val texture = ResourceLocation("textures/entity/chest/ender.png")
        val shader = Shader("/assets/trollhack/shaders/tileentity/EnderChest.vsh", "/assets/trollhack/shaders/tileentity/Default.fsh")
    }
}