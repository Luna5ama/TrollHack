package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.manager.managers.TimerManager.resetTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.MovementUtils
import cum.xiaro.trollhack.util.MovementUtils.calcMoveYaw
import cum.xiaro.trollhack.util.accessor.setFlag
import cum.xiaro.trollhack.util.inventory.slot.chestSlot
import cum.xiaro.trollhack.util.world.getGroundLevel
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketEntityAction
import kotlin.math.cos
import kotlin.math.sin

internal object ElytraFlightNew : Module(
    name = "ElytraFlightNew",
    description = "Allows infinite and way easier Elytra flying",
    category = Category.MOVEMENT,
    modulePriority = 1000
) {
    private val jumpTimer by setting("Jump Timer", 0.5f, 0.1f..2.0f, 0.01f)
    private val fallTimer by setting("Fall Timer", 0.25f, 0.1f..2.0f, 0.01f)
    private val boostTimer by setting("Boost Timer", 1.08f, 1.0f..2.0f, 0.01f)
    private val minTakeoffHeight by setting("Min Takeoff Height", 0.8f, 0.0f..1.5f, 0.1f)
    private val speed by setting("Speed", 2.5f, 0.1f..10.0f, 0.05f)
    private val upSpeed by setting("Up Speed", 2.5f, 0.1f..10.0f, 0.05f)
    private val downSpeed by setting("Down Speed", 2.5f, 0.1f..10.0f, 0.05f)

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

        if (MovementUtils.isInputting) {
            val yaw = calcMoveYaw()
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
                event.y = upSpeed.toDouble()
            } else {
                event.y = -downSpeed.toDouble()
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