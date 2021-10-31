package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.rightClickDelayTimer
import cum.xiaro.trollhack.util.atFalse
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.or
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand

internal object FastUse : Module(
    name = "FastUse",
    category = Category.PLAYER,
    description = "Use items faster"
) {
    val multiUse by setting("Multi Use", 1, 1..10, 1)
    val delay by setting("Delay", 0, 0..10, 1)
    private val blocks = setting("Blocks", false)
    private val allItems = setting("All Items", false)
    private val expBottles = setting("Exp Bottles", true, allItems.atFalse())
    private val endCrystals = setting("End Crystals", true, allItems.atFalse())
    private val fireworks = setting("Fireworks", false, allItems.atFalse())
    private val bow = setting("Bow", true, allItems.atFalse())
    private val chargeSetting = setting("Bow Charge", 3, 0..20, 1, allItems.atTrue() or bow.atTrue())
    private val chargeVariation = setting("Charge Variation", 5, 0..20, 1, allItems.atTrue() or bow.atTrue())

    private var lastUsedHand = EnumHand.MAIN_HAND
    private var randomVariation = 0

    val bowCharge get() = if (isEnabled && (allItems.value || bow.value)) 72000.0 - (chargeSetting.value.toDouble() + chargeVariation.value / 2.0) else null

    init {
        safeListener<TickEvent.Post> {
            if (player.isSpectator) return@safeListener

            if ((allItems.value || bow.value) && player.isHandActive && (player.activeItemStack.item == Items.BOW) && player.itemInUseMaxCount >= getBowCharge()) {
                randomVariation = 0
                playerController.onStoppedUsingItem(player)
            }
        }

        listener<PacketEvent.PostSend> {
            if (it.packet is CPacketPlayerTryUseItem) lastUsedHand = it.packet.hand
            else if (it.packet is CPacketPlayerTryUseItemOnBlock) lastUsedHand = it.packet.hand
        }
    }

    private fun getBowCharge(): Int {
        if (randomVariation == 0) {
            randomVariation = if (chargeVariation.value == 0) 0 else (0..chargeVariation.value).random()
        }
        return chargeSetting.value + randomVariation
    }

    @JvmStatic
    fun updateRightClickDelay() {
        if (isEnabled) {
            runSafe {
                val item = player.getHeldItem(lastUsedHand).item
                if (item !is ItemAir
                    && (allItems.value && item !is ItemBlock
                        || blocks.value && item is ItemBlock
                        || expBottles.value && item is ItemExpBottle
                        || endCrystals.value && item is ItemEndCrystal
                        || fireworks.value && item is ItemFirework)) {
                    mc.rightClickDelayTimer = delay
                }
            }
        }
    }
}