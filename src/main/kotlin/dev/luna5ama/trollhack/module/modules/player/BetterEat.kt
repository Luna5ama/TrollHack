package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.CircularArray
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.ProcessKeyBindEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.accessor.entityID
import dev.luna5ama.trollhack.util.accessor.onItemUseFinish
import dev.luna5ama.trollhack.util.accessor.syncCurrentPlayItem
import dev.luna5ama.trollhack.util.collections.averageOrZero
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketUpdateHealth
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos

internal object BetterEat : Module(
    name = "Better Eat",
    description = "Optimize eating",
    category = Category.PLAYER
) {
    private val toggleEat0 = setting("Toggle Eat", false)
    private val toggleEat by toggleEat0
    private val displayEatDelay by setting("Display Eat Delay", false)
    private val delay by setting("Delay", 50, 0..500, 5)

    private val eatTimeArray = CircularArray<Int>(5)
    private val eatTimer = TickTimer()
    private val doubleClickTimer = TickTimer()
    private val spamTimer = TickTimer()

    private var toggled = false
    private var eating = false
    private var lastEatTime = 0L
    private var slot = -1

    override fun getHudInfo(): String {
        return if (displayEatDelay) "${eatTimeArray.averageOrZero().toInt()} ms"
        else ""
    }

    init {
        onDisable {
            eatTimeArray.clear()
            eatTimer.reset(-69420L)

            toggled = false
            eating = false
            lastEatTime = 0L
            slot = -1
        }

        safeListener<PacketEvent.Receive> { event ->
            when (event.packet) {
                is SPacketEntityStatus -> {
                    if (eating && event.packet.opCode.toInt() == 9 && event.packet.entityID == mc.player?.entityId) {
                        event.cancel()
                        onMainThreadSafe {
                            if (player.isEating()) {
                                val hand = player.activeHand
                                player.onItemUseFinish()
                                connection.sendPacket(CPacketPlayerTryUseItem(hand))
                            }
                        }
                    }
                }
                is SPacketUpdateHealth -> {
                    if (event.packet.health >= player.health && event.packet.foodLevel >= player.foodStats.foodLevel) {
                        eatTimer.reset()
                    }
                }
                is SPacketSoundEffect -> {
                    if (event.packet.category == SoundCategory.PLAYERS
                        && event.packet.sound == SoundEvents.ENTITY_PLAYER_BURP
                        && player.distanceSqTo(event.packet.x, event.packet.y, event.packet.z) <= 2.0
                    ) {
                        if (!eatTimer.tick(1L)
                            || !eatTimer.tick(25L) && checkPlayers()
                        ) {
                            val current = System.currentTimeMillis()
                            eatTimeArray.add((current - lastEatTime).toInt())
                            lastEatTime = current
                            eatTimer.reset(-69420L)
                        }
                    }
                }
            }
        }

        safeListener<InputEvent.Mouse> { event ->
            if (toggleEat && event.button == 1 && !event.state) {
                if (doubleClickTimer.tickAndReset(250L)) {
                    toggled = !toggled
                    if (toggled) slot = player.inventory.currentItem
                } else {
                    toggled = false
                    playerController.syncCurrentPlayItem()
                    connection.sendPacket(
                        CPacketPlayerDigging(
                            CPacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                    player.stopActiveHand()
                }
            }
        }

        safeListener<ProcessKeyBindEvent.Pre> {
            val flag = player.inventory.currentItem == slot
                && (!player.isHandActive || player.activeItemStack.item == Items.GOLDEN_APPLE)
                && EnumHand.values().any { isValidItem(player.getHeldItem(it).item) }

            if (toggleEat) {
                if (flag) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, toggled)
                } else if (toggled) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                    slot = -1
                }
            }

            toggled = toggled && flag
        }

        safeListener<ProcessKeyBindEvent.Post> {
            val playerEating = player.isEating()
            eating = mc.gameSettings.keyBindUseItem.isKeyDown && playerEating

            if (eating && spamTimer.tickAndReset(delay)) {
                connection.sendPacket(CPacketPlayerTryUseItem(player.activeHand))
            }

            if (!playerEating || player.activeItemStack.item !is ItemFood) {
                lastEatTime = System.currentTimeMillis()
            }
        }
    }

    private fun SafeClientEvent.checkPlayers(): Boolean {
        return EntityManager.players.none {
            it.isEntityAlive && !it.isFakeOrSelf && it.entityBoundingBox.intersects(player.entityBoundingBox)
        }
    }

    @JvmStatic
    fun shouldCancelStopUsingItem(): Boolean {
        return isEnabled
            && mc.player?.isEating() ?: false
    }

    @JvmStatic
    private fun EntityPlayerSP.isEating(): Boolean {
        return this.isHandActive
            && isValidItem(this.activeItemStack.item)
    }

    private fun isValidItem(item: Item): Boolean {
        return item is ItemFood || item == Items.POTIONITEM
    }

    init {
        toggleEat0.valueListeners.add { _, it ->
            if (!it) toggled = false
        }
    }
}