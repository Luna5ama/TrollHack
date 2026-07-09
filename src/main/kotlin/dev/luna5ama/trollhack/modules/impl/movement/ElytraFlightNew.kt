package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.player.PlayerMoveEvent
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.manager.managers.TimerManager.resetTimer
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.combat.MovementUtils
import dev.luna5ama.trollhack.utils.combat.MovementUtils.calcMoveYaw
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import dev.luna5ama.trollhack.utils.inventory.chestSlot
import dev.luna5ama.trollhack.utils.world.getGroundPos
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.item.Items
import kotlin.math.cos
import kotlin.math.sin

object ElytraFlightNew : Module(
    name = "Elytra Flight New",
    description = "Allows infinite and way easier Elytra flying",
    category = Category.MOVEMENT,
    modulePriority = 1000
) {
    private val jumpTimer by setting("Jump Timer", 0.5f, 0.1f..2.0f, 0.01f)
    private val fallTimer by setting("Fall Timer", 0.25f, 0.1f..2.0f, 0.01f)
    private val boostTimer by setting("Boost Timer", 1.08f, 1.0f..2.0f, 0.01f)
    private val minTakeoffHeight by setting("Min Takeoff Height", 0.8f, 0.0f..1.5f, 0.1f)
    private val speed by setting("Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val speedFast by setting("Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)
    private val upSpeed by setting("Up Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val upSpeedFast by setting("Up Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)
    private val downSpeed by setting("Down Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val downSpeedFast by setting("Down Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)

    private var state = State.ON_GROUND

    init {
        onDisabled {
            state = State.ON_GROUND
        }

        nonNullHandler<PlayerMoveEvent.Pre> {
            updateState()

            when (state) {
                State.ON_GROUND -> onGround()
                State.TAKEOFF -> takeoff()
                State.FLYING -> fly(it)
            }
        }
    }

    private fun NonNullContext.updateState() {
        state = when {
            player.onGround() || player.chestSlot.item.item != Items.ELYTRA -> State.ON_GROUND
            player.isFallFlying -> State.FLYING
            else -> State.TAKEOFF
        }
    }

    private fun NonNullContext.onGround() {
        player.setSharedFlag(7, false)
        resetTimer()
    }

    private fun NonNullContext.takeoff() {
        if (player.velocityY < 0.0) {
            if (player.y - world.getGroundPos(player).y > minTakeoffHeight) {
                netHandler.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING))
                modifyTimer(50.0f / fallTimer)
            } else {
                modifyTimer(25.0f)
            }
        } else {
            modifyTimer(50.0f / jumpTimer)
        }
    }

    private fun NonNullContext.fly(event: PlayerMoveEvent.Pre) {
        player.setDeltaMovement(player.velocityX, 0.0, player.velocityZ)

        val sprint = mc.options.keySprint.isDown

        if (MovementUtils.isInputting()) {
            val yaw = player.calcMoveYaw()
            val speed = if (sprint) speedFast else speed
            event.x = -sin(yaw) * speed
            event.z = cos(yaw) * speed
            modifyTimer(50.0f / boostTimer)
        } else {
            event.x = 0.0
            event.z = 0.0
            resetTimer()
        }

        val jump = player.input.keyPresses.jump
        val sneak = player.input.keyPresses.shift

        if (jump xor sneak) {
            if (jump) {
                event.y = (if (sprint) upSpeedFast else upSpeed).toDouble()
            } else {
                event.y = -(if (sprint) downSpeedFast else downSpeed).toDouble()
            }
        }

        player.setDeltaMovement(0.0, 0.0, 0.0)
    }

    private enum class State {
        ON_GROUND, TAKEOFF, FLYING
    }
}