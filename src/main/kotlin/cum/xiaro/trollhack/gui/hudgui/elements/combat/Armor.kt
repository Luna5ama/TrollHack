package cum.xiaro.trollhack.gui.hudgui.elements.combat

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.gui.hudgui.HudElement
import cum.xiaro.trollhack.util.graphics.HAlign
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.VAlign
import cum.xiaro.trollhack.util.graphics.color.ColorGradient
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.inventory.slot.allSlots
import cum.xiaro.trollhack.util.inventory.slot.countItem
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import kotlin.math.max

internal object Armor : HudElement(
    name = "Armor",
    category = Category.COMBAT,
    description = "Show the durability of armor and the count of them"
) {

    private val classic by setting("Classic", false)
    private val armorCount by setting("ArmorCount", true)
    private val countElytras by setting("CountElytras", false, { armorCount })
    private val durabilityPercentage by setting("Durability Percentage", true)
    private val durabilityBar by setting("Durability Bar", false)

    override val hudWidth: Float
        get() = if (classic) {
            80.0f
        } else {
            renderStringWidth
        }

    override val hudHeight: Float
        get() = if (classic) {
            40.0f
        } else {
            80.0f
        }

    private var stringWidth = 120.0f
    private var renderStringWidth = 120.0f

    private val armorCounts = IntArray(4)
    private val duraColorGradient = ColorGradient(
        ColorGradient.Stop(0f, ColorRGB(200, 20, 20)),
        ColorGradient.Stop(50f, ColorRGB(240, 220, 20)),
        ColorGradient.Stop(100f, ColorRGB(20, 232, 20))
    )

    init {
        safeParallelListener<TickEvent.Post> {
            val slots = player.allSlots

            armorCounts[0] = slots.countItem(Items.DIAMOND_HELMET)
            armorCounts[1] = slots.countItem(
                if (countElytras && player.inventory.getStackInSlot(38).item == Items.ELYTRA) Items.ELYTRA
                else Items.DIAMOND_CHESTPLATE
            )
            armorCounts[2] = slots.countItem(Items.DIAMOND_LEGGINGS)
            armorCounts[3] = slots.countItem(Items.DIAMOND_BOOTS)
        }
    }

    override fun renderHud() {
        super.renderHud()
        stringWidth = 0.0f

        runSafe {
            GlStateManager.pushMatrix()

            for ((index, itemStack) in player.armorInventoryList.reversed().withIndex()) {
                if (classic) {
                    drawClassic(index, itemStack)
                } else {
                    drawModern(index, itemStack)
                }
            }

            GlStateManager.popMatrix()
        }

        renderStringWidth = stringWidth + 24.0f
    }

    private fun drawClassic(index: Int, itemStack: ItemStack) {
        val itemY = if (dockingV != VAlign.TOP) (MainFontRenderer.getHeight() + 4.0f).toInt() else 2

        drawItem(itemStack, index, 2, itemY)
        GlStateManager.translate(20.0f, 0.0f, 0.0f)
    }

    private fun drawModern(index: Int, itemStack: ItemStack) {
        val itemX = if (dockingH != HAlign.RIGHT) 2 else (renderStringWidth - 18.0f).toInt()

        drawItem(itemStack, index, itemX, 2)
        GlStateManager.translate(0.0f, 20.0f, 0.0f)
    }

    private fun drawItem(itemStack: ItemStack, index: Int, x: Int, y: Int) {
        if (itemStack.isEmpty) return

        RenderUtils2D.drawItem(itemStack, x, y, drawOverlay = false)
        drawDura(itemStack, x, y)

        if (armorCount) {
            val string = armorCounts[index].toString()
            val width = MainFontRenderer.getWidth(string)
            val height = MainFontRenderer.getHeight()

            MainFontRenderer.drawString(string, x + 16.0f - width, y + 16.0f - height)
        }
    }

    private fun drawDura(itemStack: ItemStack, x: Int, y: Int) {
        if (!itemStack.isItemStackDamageable) return

        val dura = itemStack.maxDamage - itemStack.itemDamage
        val duraMultiplier = dura / itemStack.maxDamage.toFloat()
        val duraPercent = MathUtils.round(duraMultiplier * 100.0f, 1)
        val color = duraColorGradient.get(duraPercent)

        if (durabilityBar) {
            val duraBarWidth = (16.0f * duraMultiplier).coerceAtLeast(0.0f)
            RenderUtils2D.drawRectFilled(Vec2f(x.toFloat(), y + 16.0f), Vec2f(x + 16.0f, y + 18.0f), ColorRGB(0, 0, 0))
            RenderUtils2D.drawRectFilled(Vec2f(x.toFloat(), y + 16.0f), Vec2f(x + duraBarWidth, y + 18.0f), color)
        }

        if (durabilityPercentage) {
            if (classic) {
                val string = duraPercent.toInt().toString()
                val width = MainFontRenderer.getWidth(string)

                val duraX = 10 - width * 0.5f
                val duraY = if (dockingV != VAlign.TOP) 2.0f else 22.0f

                MainFontRenderer.drawString(string, duraX, duraY, color = color)
            } else {
                val string = "$dura/${itemStack.maxDamage}  ($duraPercent%)"
                val width = MainFontRenderer.getWidth(string)
                stringWidth = max(width, stringWidth)

                val duraX = if (dockingH != HAlign.RIGHT) 22.0f else renderStringWidth - 22.0f - width
                val duraY = 10.0f - MainFontRenderer.getHeight() * 0.5f

                MainFontRenderer.drawString(string, duraX, duraY, color = color)
            }
        }
    }
}