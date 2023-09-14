package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.manager.managers.TimerManager.resetTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.MovementUtils.calcMoveYaw
import dev.luna5ama.trollhack.util.accessor.setFlag
import dev.luna5ama.trollhack.util.inventory.slot.chestSlot
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketEntityAction
import kotlin.math.cos
import kotlin.math.sin

internal object ElytraFlightNew : Module(
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

    override fun isActive(): Boolean {
        return isEnabled && state != State.ON_GROUND
    }

    init {
        onDisable {
            state = State.ON_GROUND
        }

        safeListener<PlayerMoveEvent.Pre> {
            updateState()

            when (state) {
                State.ON_GROUND -> onGround()
                State.TAKEOFF -> takeoff()
                State.FLYING -> fly(it)
            }
        }
    }

    private fun SafeClientEvent.updateState() {
        state = when {
            player.onGround || player.chestSlot.stack.item != Items.ELYTRA -> State.ON_GROUND
            player.isElytraFlying -> State.FLYING
            else -> State.TAKEOFF
        }
    }

    private fun SafeClientEvent.onGround() {
        player.setFlag(7, false)
        resetTimer()
    }

    private fun SafeClientEvent.takeoff() {
        if (player.motionY < 0.0) {
            if (player.posY - world.getGroundLevel(player) > minTakeoffHeight) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING))
                modifyTimer(50.0f / fallTimer)
            } else {
                modifyTimer(25.0f)
            }
        } else {
            modifyTimer(50.0f / jumpTimer)
        }
    }

    private fun SafeClientEvent.fly(event: PlayerMoveEvent.Pre) {
        player.motionY = 0.0

        val sprint = mc.gameSettings.keyBindSprint.isKeyDown

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

        val jump = player.movementInput.jump
        val sneak = player.movementInput.sneak

        if (jump xor sneak) {
            if (jump) {
                event.y = (if (sprint) upSpeedFast else upSpeed).toDouble()
            } else {
                event.y = -(if (sprint) downSpeedFast else downSpeed).toDouble()
            }
        }

        player.motionX = 0.0
        player.motionY = 0.0
        player.motionZ = 0.0
    }

    private enum class State {
        ON_GROUND, TAKEOFF, FLYING
    }
}