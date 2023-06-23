package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.inventory.ItemStackPredicate
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.threads.runSafeOrElse
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

internal object CombatItemCount : LabelHud(
    name = "Combat Item Count",
    category = Category.COMBAT,
    description = "Counts combat items like gapples, crystal, etc"
) {
    private val itemSettings = arrayOf(
        ItemSetting("Arrow", ItemStackPredicate.byItem(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)),
        ItemSetting("Bed", ItemStackPredicate.byItem(Items.BED)) { player.dimension != 0 },
        ItemSetting("Crystal", ItemStackPredicate.byItem(Items.END_CRYSTAL)),
        ItemSetting("Gapple", ItemStackPredicate.byItem(Items.GOLDEN_APPLE)),
        ItemSetting("Totem", ItemStackPredicate.byItem(Items.TOTEM_OF_UNDYING)),
        ItemSetting("Xp Bottle", ItemStackPredicate.byItem(Items.EXPERIENCE_BOTTLE)),
        ItemSetting("Pearl", ItemStackPredicate.byItem(Items.ENDER_PEARL)),
        ItemSetting("Chorus Fruit", ItemStackPredicate.byItem(Items.CHORUS_FRUIT))
    )

    private val showIcon by setting("Show Icon", true)
    private val horizontal by setting("Horizontal", true, { showIcon })

    private val itemStacks = arrayOf(
        ItemStack(Items.ARROW, -1),
        ItemStack(Items.BED, -1),
        ItemStack(Items.END_CRYSTAL, -1),
        ItemStack(Items.GOLDEN_APPLE, -1, 1),
        ItemStack(Items.TOTEM_OF_UNDYING, -1),
        ItemStack(Items.EXPERIENCE_BOTTLE, -1),
        ItemStack(Items.ENDER_PEARL, -1),
        ItemStack(Items.CHORUS_FRUIT, -1)
    )

    override val hudWidth by FrameFloat {
        if (showIcon) {
            if (horizontal) 20.0f * itemSettings.count { it.isEnabled() }
            else 20.0f
        } else {
            displayText.getWidth()
        }
    }

    override val hudHeight by FrameFloat {
        if (showIcon) {
            if (horizontal) 20.0f
            else 20.0f * itemSettings.count { it.isEnabled() }
        } else {
            displayText.getHeight(2)
        }
    }

    override fun SafeClientEvent.updateText() {
        val slots = player.allSlots

        for ((index, entry) in itemSettings.withIndex()) {
            val count = if (entry.isEnabled()) {
                slots.asSequence()
                    .filter { entry.predicate(it.stack) }
                    .sumOf { it.stack.count }
            } else {
                -1
            }

            if (showIcon) {
                itemStacks[index].count = count + 1 // Weird way to get around Minecraft item count check
            } else if (count > -1) {
                displayText.add(entry.name, GuiSetting.text)
                displayText.addLine("x$count", GuiSetting.primary)
            }
        }
    }

    override fun renderHud() {
        if (showIcon) {
            GlStateManager.pushMatrix()

            for (itemStack in itemStacks) {
                if (itemStack.count == 0) continue
                RenderUtils2D.drawItem(itemStack, 2, 2, (itemStack.count - 1).toString())
                if (horizontal) GlStateManager.translate(20.0f, 0.0f, 0.0f)
                else GlStateManager.translate(0.0f, 20.0f, 0.0f)
            }

            GlStateManager.popMatrix()
        } else {
            super.renderHud()
        }
    }

    private class ItemSetting(
        val name: String,
        val predicate: ItemStackPredicate,
        private val enabled: (SafeClientEvent.() -> Boolean)? = null
    ) {
        private val setting = setting(name, true)

        fun isEnabled(): Boolean {
            return runSafeOrElse(false) { setting.value && (enabled?.invoke(this) ?: true) }
        }
    }
}