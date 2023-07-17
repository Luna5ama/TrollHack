package dev.luna5ama.trollhack.module.modules.movement

import baritone.api.pathing.goals.GoalXZ
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.baritone.BaritoneCommandEvent
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.LagNotifier
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.Direction
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.util.MovementInputFromOptions

internal object AutoWalk : Module(
    name = "Auto Walk",
    category = Category.MOVEMENT,
    description = "Automatically walks somewhere"
) {
    private val mode = setting("Direction", AutoWalkMode.BARITONE)
    private val disableOnDisconnect by setting("Disable On Disconnect", true)

    private enum class AutoWalkMode(override val displayName: CharSequence) : DisplayEnum {
        FORWARD("Forward"),
        BACKWARD("Backward"),
        BARITONE("Baritone")
    }

    val baritoneWalk get() = isEnabled && mode.value == AutoWalkMode.BARITONE

    private const val border = 30000000
    private val messageTimer = TickTimer(TimeUnit.SECONDS)
    var direction = Direction.NORTH; private set

    override fun isActive(): Boolean {
        return isEnabled && (mode.value != AutoWalkMode.BARITONE || BaritoneUtils.isActive || BaritoneUtils.isPathing)
    }

    override fun getHudInfo(): String {
        return if (mode.value == AutoWalkMode.BARITONE && (BaritoneUtils.isActive || BaritoneUtils.isPathing)) {
            direction.displayString
        } else {
            mode.value.displayString
        }
    }

    init {
        onDisable {
            if (mode.value == AutoWalkMode.BARITONE) BaritoneUtils.cancelEverything()
        }

        listener<BaritoneCommandEvent> {
            if (it.command.contains("cancel")) {
                disable()
            }
        }

        listener<ConnectionEvent.Disconnect> {
            if (disableOnDisconnect) disable()
        }

        listener<InputUpdateEvent>(6969) {
            if (LagNotifier.paused && LagNotifier.pauseAutoWalk) return@listener

            if (it.movementInput !is MovementInputFromOptions) return@listener

            when (mode.value) {
                AutoWalkMode.FORWARD -> {
                    it.movementInput.moveForward = 1.0f
                }
                AutoWalkMode.BACKWARD -> {
                    it.movementInput.moveForward = -1.0f
                }
                else -> {
                    // Baritone mode
                }
            }
        }

        safeListener<TickEvent.Pre> {
            if (mode.value == AutoWalkMode.BARITONE && !checkBaritoneElytra() && !isActive()) {
                startPathing()
            }
        }
    }

    private fun SafeClientEvent.startPathing() {
        if (!world.isChunkGeneratedAt(player.chunkCoordX, player.chunkCoordZ)) return

        direction = Direction.fromEntity(player)
        val x = player.posX.floorToInt() + direction.directionVec.x * border
        val z = player.posZ.floorToInt() + direction.directionVec.z * border

        BaritoneUtils.cancelEverything()
        BaritoneUtils.primary?.customGoalProcess?.setGoalAndPath(GoalXZ(x, z))
    }

    private fun checkBaritoneElytra() = mc.player?.let {
        if (it.isElytraFlying && messageTimer.tickAndReset(10L)) {
            NoSpamMessage.sendError(
                "$chatName Baritone mode isn't currently compatible with Elytra flying!" +
                    " Choose a different mode if you want to use AutoWalk while Elytra flying"
            )
        }
        it.isElytraFlying
    } ?: true

    init {
        mode.listeners.add {
            if (isDisabled || mc.player == null) return@add
            if (mode.value == AutoWalkMode.BARITONE) {
                if (!checkBaritoneElytra()) {
                    runSafe { startPathing() }
                }
            } else {
                BaritoneUtils.cancelEverything()
            }
        }
    }
}