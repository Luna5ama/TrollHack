package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.entity.player.Input

object Disabler : Module("Disabler", category = Category.PLAYER) {
    private val badPacketsA by setting("BadPacketsA", true)
    private val sprinting by setting("Sprinting", true)
    private val input by setting("Input", true)

    private var lastSlot = -1
    private var replaying = false
    private var oldInput: Input? = null

    init {
        onEnabled {
            lastSlot = -1
            replaying = false
            oldInput = null
        }
        onDisabled { restoreInput() }

        nonNullHandler<PacketEvent.Send> { event ->
            if (replaying) return@nonNullHandler

            when (val packet = event.packet) {
                is ServerboundSetCarriedItemPacket -> {
                    if (badPacketsA && packet.slot == lastSlot && packet.slot != -1) {
                        event.cancel()
                    } else {
                        lastSlot = packet.slot
                    }
                }

                is ServerboundContainerClickPacket, is ServerboundContainerClosePacket -> {
                    event.cancel()
                    val wasSprinting = player.isSprinting

                    if (input) spoofInput()
                    if (sprinting && wasSprinting) setSprinting(false)

                    replaying = true
                    try {
                        netHandler.send(packet)
                    } finally {
                        replaying = false
                    }

                    if (sprinting && wasSprinting) setSprinting(true)
                    if (input) restoreInput()
                }
            }
        }
    }

    private fun spoofInput() {
        val player = mc.player ?: return
        if (oldInput != null) return
        oldInput = player.input.keyPresses
        player.input.keyPresses = Input.EMPTY
        mc.connection?.send(ServerboundPlayerInputPacket(Input.EMPTY))
    }

    private fun restoreInput() {
        val previous = oldInput ?: return
        mc.player?.input?.keyPresses = previous
        oldInput = null
    }

    private fun setSprinting(state: Boolean) {
        val player = mc.player ?: return
        player.isSprinting = state
        mc.connection?.send(
            ServerboundPlayerCommandPacket(
                player,
                if (state) ServerboundPlayerCommandPacket.Action.START_SPRINTING
                else ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
            )
        )
    }
}
