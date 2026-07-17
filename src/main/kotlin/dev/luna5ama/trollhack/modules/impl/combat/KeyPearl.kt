package dev.luna5ama.trollhack.modules.impl.combat

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.inventory.InventoryUtils
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

object KeyPearl : Module("Key Pearl", category = Category.COMBAT) {
    private val activateKey by setting("Activate Key", GLFW.GLFW_KEY_UNKNOWN)
    private val delay by setting("Delay", 0, 0..20, 1)
    private val switchBack by setting("Switch Back", true)
    private val switchDelay by setting("Switch Delay", 0, 0..20, 1)
    private val swingHand by setting("Swing Hand", true)

    private var active = false
    private var used = false
    private var clock = 0
    private var switchClock = 0
    private var previousSlot = -1
    private var held = false

    init {
        onEnabled {
            reset()
            held = false
        }
        onDisabled {
            reset()
            held = false
        }
        nonNullHandler<TickEvent.Pre> {
            if (mc.screen != null) return@nonNullHandler
            val down = activateKey != GLFW.GLFW_KEY_UNKNOWN &&
                GLFW.glfwGetKey(mc.window.handle(), activateKey) == GLFW.GLFW_PRESS
            if (!down) held = false
            if (down && !held) active = true
            held = down
            if (!active) return@nonNullHandler

            if (previousSlot == -1) previousSlot = player.inventory.selectedSlot
            val pearl = InventoryUtils.findItem(Items.ENDER_PEARL)
            if (pearl == -1) {
                reset()
                return@nonNullHandler
            }
            player.inventory.selectedSlot = pearl
            netHandler.send(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(pearl))
            if (clock++ < delay) return@nonNullHandler
            if (!used) {
                val result = interaction.useItem(player, InteractionHand.MAIN_HAND)
                if (result.consumesAction()) {
                    if (swingHand) player.swing(InteractionHand.MAIN_HAND)
                    else netHandler.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
                }
                used = true
            }
            if (!switchBack) {
                reset()
                return@nonNullHandler
            }
            if (switchClock++ < switchDelay) return@nonNullHandler
            if (previousSlot >= 0) {
                player.inventory.selectedSlot = previousSlot
                netHandler.send(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(previousSlot))
            }
            reset()
        }
    }

    private fun reset() {
        active = false
        used = false
        clock = 0
        switchClock = 0
        previousSlot = -1
    }
}
