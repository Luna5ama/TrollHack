package dev.luna5ama.trollhack.modules.impl.combat

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items

object AutoTotem : Module("Auto Totem", category = Category.COMBAT) {
    private val strict by setting("Strict", true)
    private val health by setting("Health", 16.0, 0.0..36.0, 0.5)
    private val checkGapple by setting("Check Gapple", true)

    init {
        nonNullHandler<TickEvent.Pre> {
            if (!shouldHoldTotem() || player.offhandItem.`is`(Items.TOTEM_OF_UNDYING)) {
                return@nonNullHandler
            }

            val slot = (0..35).firstOrNull { player.inventory.getItem(it).`is`(Items.TOTEM_OF_UNDYING) }
                ?: return@nonNullHandler
            val menuSlot = if (slot < 9) slot + 36 else slot
            val menu = player.inventoryMenu

            if (!strict) {
                interaction.handleContainerInput(menu.containerId, menuSlot, 40, ContainerInput.SWAP, player)
                return@nonNullHandler
            }

            interaction.handleContainerInput(menu.containerId, menuSlot, 0, ContainerInput.PICKUP, player)
            interaction.handleContainerInput(menu.containerId, 45, 0, ContainerInput.PICKUP, player)
            if (!menu.carried.isEmpty) {
                interaction.handleContainerInput(menu.containerId, menuSlot, 0, ContainerInput.PICKUP, player)
            }
        }
    }

    private fun shouldHoldTotem(): Boolean {
        val player = mc.player ?: return false
        if (player.health + player.absorptionAmount <= health.toFloat()) return true
        if (player.offhandItem.isEmpty) return true
        if (player.y < -64.0) return true
        if (!checkGapple) return false

        return player.mainHandItem.`is`(Items.GOLDEN_APPLE) ||
            player.mainHandItem.`is`(Items.ENCHANTED_GOLDEN_APPLE)
    }
}
