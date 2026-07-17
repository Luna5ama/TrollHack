package dev.luna5ama.trollhack.modules.impl.combat

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.doSwap
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.rotation.Priority
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items

object AutoMend : Module("Auto Mend", category = Category.COMBAT) {
    private val switchMode by setting("Switch Mode", SwitchMode.NORMAL)
    private val swingHand by setting("Swing Hand", false)
    private var shouldSwapBack = false
    private var previousSlot = -1

    init {
        onEnabled {
            shouldSwapBack = false
            previousSlot = -1
        }
        onDisabled {
            val player = mc.player
            if (shouldSwapBack && previousSlot in 0..8 && player != null) {
                player.inventory.selectedSlot = previousSlot
                mc.connection?.send(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(previousSlot))
            }
            shouldSwapBack = false
            previousSlot = -1
        }
        nonNullHandler<TickEvent.Pre> {
            val slot = player.hotbarSlots.firstOrNull { it.item.`is`(Items.EXPERIENCE_BOTTLE) } ?: return@nonNullHandler
            RotationManager.setRotations(Vec2f(player.yRot, 90f), priority = Priority.High)

            val useBottle = {
                interaction.useItem(player, InteractionHand.MAIN_HAND)
                if (swingHand) player.swing(InteractionHand.MAIN_HAND)
                else netHandler.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
            }

            if (switchMode == SwitchMode.SILENT) {
                ghostSwitch(slot, useBottle)
            } else {
                if (!shouldSwapBack) previousSlot = player.inventory.selectedSlot
                doSwap(slot.hotbarSlot)
                useBottle()
                shouldSwapBack = true
            }
        }
    }

    private enum class SwitchMode(override val displayName: CharSequence) : Displayable {
        NORMAL("Normal"),
        SILENT("Silent")
    }
}
