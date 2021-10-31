package cum.xiaro.trollhack.util.graphics.fastrender.tileentity

import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.shaders.DrawShader
import cum.xiaro.trollhack.util.graphics.use
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import java.nio.ByteBuffer

abstract class AbstractTileEntityRenderBuilder<T : TileEntity>(
    protected val builtPosX: Double,
    protected val builtPosY: Double,
    protected val builtPosZ: Double
) : ITileEntityRenderBuilder<T> {
    protected var size = 0

    protected val floatArrayList by lazy {
        FloatArrayList()
    }
    protected val shortArrayList by lazy {
        ShortArrayList()
    }
    protected val byteArrayList by lazy {
        ByteArrayList()
    }

    private var buffer: ByteBuffer? = null

    override fun build() {
        buffer = buildBuffer()
    }

    override fun upload(): ITileEntityRenderBuilder.Renderer {
        return uploadBuffer(buffer ?: buildBuffer())
    }

    protected abstract fun buildBuffer(): ByteBuffer

    protected abstract fun uploadBuffer(buffer: ByteBuffer): Renderer

    protected fun getTileEntityBlockMetadata(tileEntity: TileEntity): Int {
        return if (tileEntity.hasWorld()) {
            tileEntity.blockMetadata
        } else {
            0
        }
    }

    protected fun putTileEntityLightMapUV(tileEntity: TileEntity) {
        @Suppress("UNNECESSARY_SAFE_CALL")
        tileEntity.world?.let {
            val i = it.getCombinedLight(tileEntity.pos, 0)
            byteArrayList.add((i and 0xFF).toByte())
            byteArrayList.add((i shr 16 and 0xFF).toByte())
        } ?: run {
            byteArrayList.add(0)
            byteArrayList.add(-16)
        }
    }

    open class Renderer(
        private val shader: Shader,
        private val vaoID: Int,
        private val vboID: Int,
        private val modelSize: Int,
        private val size: Int,
        private val builtPosX: Double,
        private val builtPosY: Double,
        private val builtPosZ: Double
    ) : ITileEntityRenderBuilder.Renderer {
        override fun render(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            shader.use {
                preRender()

                val x = builtPosX - renderPosX
                val y = builtPosY - renderPosY
                val z = builtPosZ - renderPosZ

                val modelView = MatrixUtils.loadModelViewMatrix().getMatrix()
                    .translate(x.toFloat(), y.toFloat(), z.toFloat())

                updateModelViewMatrix(modelView)

                glBindVertexArray(vaoID)
                glDrawArraysInstanced(GL_TRIANGLES, 0, modelSize, size)
                glBindVertexArray(0)

                postRender()
            }
        }

        protected open fun preRender() {

        }

        protected open fun postRender() {

        }

        override fun destroy() {
            glDeleteVertexArrays(vaoID)
            glDeleteBuffers(vboID)
            glDeleteBuffers(vboID)
        }
    }

    open class Shader(vertShaderPath: String, fragShaderPath: String) : DrawShader(vertShaderPath, fragShaderPath) {
        private val partialTicksUniform = glGetUniformLocation(id, "partialTicks")

        init {
            use {
                glUniform1i(glGetUniformLocation(id, "lightMapTexture"), 1)
            }

            @Suppress("LeakingThis")
            shaders.add(this)
        }

        protected open fun update() {
            uploadProjectionMatrix(MatrixUtils.matrixBuffer)
            glUniform1f(partialTicksUniform, RenderUtils3D.partialTicks)
        }

        companion object {
            @JvmStatic
            private val shaders = ArrayList<Shader>()

            @JvmStatic
            fun updateShaders() {
                MatrixUtils.loadProjectionMatrix()

                shaders.forEach {
                    it.use {
                        update()
                    }
                }
            }
        }
    }
}