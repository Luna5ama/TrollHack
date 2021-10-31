package cum.xiaro.trollhack.util.graphics.fastrender.tileentity

import net.minecraft.tileentity.TileEntity

interface ITileEntityRenderBuilder<T : TileEntity> {
    fun add(tileEntity: T)

    fun build()

    fun upload(): Renderer

    interface Renderer {
        fun render(renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        fun destroy()
    }
}