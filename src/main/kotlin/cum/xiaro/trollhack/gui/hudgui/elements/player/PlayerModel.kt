package cum.xiaro.trollhack.gui.hudgui.elements.player

import cum.xiaro.trollhack.gui.hudgui.HudElement
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL11.*

internal object PlayerModel : HudElement(
    name = "PlayerModel",
    category = Category.PLAYER,
    description = "Your player icon, or players you attacked"
) {
    private val emulatePitch by setting("Emulate Pitch", true)
    private val emulateYaw by setting("Emulate Yaw", false)

    override val hudWidth: Float get() = 50.0f
    override val hudHeight: Float get() = 80.0f

    override val resizable: Boolean = true

    override fun renderHud() {
        if (mc.renderManager.renderViewEntity == null) return

        super.renderHud()
        runSafe {
            val yaw = if (emulateYaw) interpolateAndWrap(player.prevRotationYaw, player.rotationYaw) else 0.0f
            val pitch = if (emulatePitch) interpolateAndWrap(player.prevRotationPitch, player.rotationPitch) else 0.0f

            glPushMatrix()
            glTranslatef(renderWidth / scale / 2.0f, renderHeight / scale - 8.0f, 0.0f)
            GlStateUtils.depth(true)
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

            GlStateUtils.useProgram(0)
            GuiInventory.drawEntityOnScreen(0, 0, 35, -yaw, -pitch, player)

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateUtils.depth(false)
            GlStateUtils.texture2d(true)
            GlStateUtils.blend(true)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

            GlStateManager.disableColorMaterial()
            glPopMatrix()
        }
    }

    private fun interpolateAndWrap(prev: Float, current: Float): Float {
        return MathHelper.wrapDegrees(prev + (current - prev) * RenderUtils3D.partialTicks)
    }
}