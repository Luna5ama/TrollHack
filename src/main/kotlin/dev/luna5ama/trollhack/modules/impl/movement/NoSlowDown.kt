package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.mixins.accessor.IClientLevelAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.movement.NoSlowDown.BypassMode.*
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.extension.*
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3

object NoSlowDown : Module("No Slow Down", "no slow down", Category.MOVEMENT) {

    private val bypassMode by setting("Mode", NCP)
    val web by setting("Web", true)
    val horizontalSpeed by setting("HSpeed", 0.7, 0.0..1.0, 0.01, { web })
    val verticalSpeed by setting("VSpeed", 0.7, 0.0..1.0, 0.01, { web })

    init {
        nonNullHandler<TickEvent.Pre> {
            when (bypassMode) {
                NCP -> {
                    if (!player.isPassenger && !player.isShiftKeyDown) {
                        netHandler.send(ServerboundSetCarriedItemPacket(player.inventory.selectedSlot))
                    }
                }
                MATRIX -> {
                    if (!player.onGround() && player.fallDistance > 0.2f) {
                        player.deltaMovement = Vec3(player.velocityX * 0.7f, player.velocityY, player.velocityZ * 0.7f)
                    }
                }
                GRIM -> {
                    if (player.usingItemHand == InteractionHand.OFF_HAND) {
                        netHandler.send(ServerboundSetCarriedItemPacket(player.inventory.selectedSlot % 8 + 1))
                        netHandler.send(ServerboundSetCarriedItemPacket(player.inventory.selectedSlot))
                    } else {
                        netHandler.send(
                            ServerboundUseItemPacket(
                                InteractionHand.MAIN_HAND,
                                getWorldActionId(world),
                                player.yaw, player.pitch
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getWorldActionId(world: ClientLevel): Int {
        val pendingUpdateManager = getUpdateManager(world)
        val actionID = pendingUpdateManager.currentSequence()
        pendingUpdateManager.close()
        return actionID
    }

    private fun getUpdateManager(world: ClientLevel): BlockStatePredictionHandler {
        return (world as IClientLevelAccessor).acquirePendingUpdateManager()
    }

    enum class BypassMode(override val displayName: CharSequence) : Displayable {
        NCP("NCP"),
        MATRIX("Matrix"),
        GRIM("Grim"),
    }
}
