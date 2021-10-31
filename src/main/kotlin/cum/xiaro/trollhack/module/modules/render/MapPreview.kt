package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.mixin.gui.MixinGuiScreen
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils2D.drawRectFilled
import cum.xiaro.trollhack.util.graphics.RenderUtils2D.drawRectOutline
import cum.xiaro.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.world.storage.MapData
import java.awt.Color

/**
 * @see MixinGuiScreen.renderToolTip
 */
internal object MapPreview : Module(
    name = "MapPreview",
    category = Category.RENDER,
    description = "Previews maps when hovering over them"
) {
    private val mapBackground = ResourceLocation("textures/map/map_background.png")

    private val showName = setting("Show Name", true)
    private val frame = setting("Show Frame", true)
    val scale = setting("Scale", 5.0, 0.0..10.0, 0.1)

    @JvmStatic
    fun getMapData(itemStack: ItemStack): MapData? {
        return (itemStack.item as? ItemMap)?.getMapData(itemStack, mc.world)
    }

    @JvmStatic
    fun drawMap(stack: ItemStack, mapData: MapData, originalX: Int, originalY: Int) {
        val x = originalX + 6.0
        val y = originalY + 6.0
        val scale = scale.value / 5.0

        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f)
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.disableDepth()

        GlStateManager.translate(x, y, 0.0)
        GlStateManager.scale(scale, scale, 0.0)

        drawMapFrame()
        mc.entityRenderer.mapItemRenderer.renderMap(mapData, false)
        drawMapName(stack)

        GlStateManager.enableDepth()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }

    private fun drawMapFrame() {
        if (!frame.value) return

        val tessellator = Tessellator.getInstance()
        val bufBuilder = tessellator.buffer
        mc.textureManager.bindTexture(mapBackground)

        // Magic numbers taken from Minecraft code
        bufBuilder.begin(7, DefaultVertexFormats.POSITION_TEX)
        bufBuilder.pos(-7.0, 135.0, 0.0).tex(0.0, 1.0).endVertex()
        bufBuilder.pos(135.0, 135.0, 0.0).tex(1.0, 1.0).endVertex()
        bufBuilder.pos(135.0, -7.0, 0.0).tex(1.0, 0.0).endVertex()
        bufBuilder.pos(-7.0, -7.0, 0.0).tex(0.0, 0.0).endVertex()

        GlStateUtils.useProgram(0)
        tessellator.draw()
    }

    private fun drawMapName(stack: ItemStack) {
        if (!showName.value) return

        val backgroundX = Vec2f(-2.0f, -18.0f)
        val backgroundY = Vec2f(
            mc.fontRenderer.FONT_HEIGHT + 4.0f,
            mc.fontRenderer.FONT_HEIGHT - 14.0f
        )

        // Draw the background
        drawRectFilled(backgroundX, backgroundY, GuiSetting.backGround)
        drawRectOutline(backgroundX, backgroundY, 1.5f, GuiSetting.outline)

        // Draw the name
        mc.fontRenderer.drawStringWithShadow(stack.displayName, 2f, -15f, Color.WHITE.rgb)
    }
}