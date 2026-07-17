package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.extension.getEnchantmentLevel
import dev.luna5ama.trollhack.utils.extension.getDamagePercent
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.phys.BlockHitResult

object AutoTool : Module("Auto Tool", category = Category.PLAYER) {
    private val swapBack by setting("Swap Back", true)
    private val saveItem by setting("Save Item", true)
    private val silent by setting("Silent", false)
    private val enderChestSilkTouch by setting("Ender Chest Silk Touch", true)
    private var oldSlot = -1
    private var swapUntil = 0L

    init {
        onDisabled {
            if (swapBack && oldSlot >= 0) restore()
        }
        nonNullHandler<TickEvent.Pre> {
            val hit = mc.hitResult as? BlockHitResult ?: return@nonNullHandler
            val state = world.getBlockState(hit.blockPos)
            if (state.isAir || !mc.options.keyAttack.isDown) {
                if (swapBack && oldSlot >= 0 && System.currentTimeMillis() >= swapUntil) restore()
                return@nonNullHandler
            }
            val best = (0..8).maxByOrNull { score(it, state) } ?: return@nonNullHandler
            if (score(best, state) <= 1.0f || best == player.inventory.selectedSlot) return@nonNullHandler
            if (oldSlot < 0) oldSlot = player.inventory.selectedSlot
            if (silent) {
                netHandler.send(ServerboundSetCarriedItemPacket(best))
            } else {
                player.inventory.selectedSlot = best
                netHandler.send(ServerboundSetCarriedItemPacket(best))
            }
            swapUntil = System.currentTimeMillis() + 300L
            if (!swapBack) oldSlot = -1
        }
    }

    private fun score(slot: Int, state: net.minecraft.world.level.block.state.BlockState): Float {
        val player = mc.player ?: return 0.0f
        val stack = player.inventory.getItem(slot)
        if (stack.isEmpty || (saveItem && stack.getDamagePercent <= 10)) return 0.0f
        if (enderChestSilkTouch && state.block is EnderChestBlock &&
            stack.getEnchantmentLevel(Enchantments.SILK_TOUCH) <= 0
        ) return 0.0f
        return stack.getDestroySpeed(state) + stack.getEnchantmentLevel(Enchantments.EFFICIENCY).toFloat()
    }

    private fun restore() {
        val slot = oldSlot
        oldSlot = -1
        val player = mc.player ?: return
        if (!silent) player.inventory.selectedSlot = slot
        mc.connection?.send(ServerboundSetCarriedItemPacket(slot))
    }
}
