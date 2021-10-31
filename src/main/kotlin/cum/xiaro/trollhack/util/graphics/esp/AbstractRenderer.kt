package cum.xiaro.trollhack.util.graphics.esp

import cum.xiaro.trollhack.util.interfaces.Helper
import cum.xiaro.trollhack.util.threads.onMainThread
import org.lwjgl.opengl.GL15.glDeleteBuffers

open class AbstractRenderer : Helper {
    protected var vaoID = 0
    protected var vboID = 0

    protected var posX = 0.0
    protected var posY = 0.0
    protected var posZ = 0.0

    var size = 0; protected set

    protected open val initialized: Boolean
        get() = vaoID != 0 && vboID != 0

    open fun clear() {
        if (initialized) {
            onMainThread {
                if (initialized) {
                    glDeleteBuffers(vboID)

                    vboID = 0
                }
            }
        }

        size = 0
        posX = 0.0
        posY = 0.0
        posZ = 0.0
    }
}

abstract class AbstractIndexedRenderer : AbstractRenderer() {
    protected var iboID = 0

    override val initialized: Boolean
        get() = vaoID != 0 && vboID != 0 && iboID != 0

    override fun clear() {
        if (initialized) {
            onMainThread {
                if (initialized) {
                    glDeleteBuffers(vboID)
                    glDeleteBuffers(iboID)

                    vboID = 0
                    iboID = 0
                }
            }
        }

        size = 0
        posX = 0.0
        posY = 0.0
        posZ = 0.0
    }
}