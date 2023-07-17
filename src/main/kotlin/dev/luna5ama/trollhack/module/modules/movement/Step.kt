package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.StepEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.combat.HolePathFinder
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.EntityUtils.isFlying
import dev.luna5ama.trollhack.util.EntityUtils.isInOrAboveLiquid
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.network.play.client.CPacketPlayer
import kotlin.math.max
import kotlin.math.min

internal object Step : Module(
    name = "Step",
    description = "Changes the vanilla behavior for stepping up blocks",
    category = Category.MOVEMENT,
    modulePriority = 200
) {
    private val mode by setting("Mode", Mode.PACKET)
    private val entityStep by setting("Entity Step", true)
    private val useTimer by setting("Use Timer", true, { mode == Mode.PACKET })
    private val strictYMotion by setting("Strict Y Motion", true)
    private val autoDisable by setting("Auto Disable", false)
    private val minHeight by setting("Min Height", 0.9f, 0.6f..2.5f, 0.1f)
    val maxHeight by setting("Max Height", 2.0f, 0.6f..2.5f, 0.1f)
    private val enableTicks by setting("Enable Ticks", 0, 0..50, 1)
    private val postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f)
    private val maxPostTicks by setting("Max Post Ticks", 40, 0..100, 1)

    const val DEFAULT_HEIGHT = 0.6f
    private var timeoutTick = Int.MIN_VALUE
    private var shouldDisable = false
    private var prevCollided = false
    private var collideTicks = 0

    @Suppress("UNUSED")
    private enum class Mode {
        VANILLA, PACKET
    }

    override fun isActive(): Boolean {
        return isEnabled && runSafe { shouldRunStep(player.motionX, player.motionY, player.motionZ) } ?: false
    }

    init {
        onDisable {
            mc.player?.apply {
                ridingEntity?.stepHeight = 1.0f
                stepHeight = DEFAULT_HEIGHT
            }
            timeoutTick = Int.MIN_VALUE
            shouldDisable = false
            prevCollided = false
            collideTicks = 0
        }

        onToggle {
            BaritoneUtils.settings?.assumeStep?.value = it
        }

        safeListener<PlayerMoveEvent.Pre>(-100, true) { event ->
            if (globalCheck()) return@safeListener

            val flag = shouldRunStep(event.x, event.y, event.z)

            if (isDisabled && (!HolePathFinder.enableStep || !HolePathFinder.isActive())) {
                if (enableTicks > 0) {
                    val collided = player.collidedHorizontally

                    if (flag && (prevCollided || collided)) collideTicks++
                    else collideTicks = 0

                    prevCollided = collided
                }

                if (enableTicks == 0 || collideTicks <= enableTicks) return@safeListener
            }

            if (isEnabled) {
                player.ridingEntity?.let {
                    it.stepHeight = if (entityStep && flag) maxHeight else 1.0f
                }
            }

            if (flag && !player.isRiding && player.onGround) {
                val stepHeight = calcStepHeight(event)
                if (!stepHeight.isNaN() && (HolePathFinder.isActive() || stepHeight >= minHeight) && stepHeight <= maxHeight) {
                    val disabled = isDisabled
                    enable()
                    when (mode) {
                        Mode.VANILLA -> vanillaStep()
                        Mode.PACKET -> packetStep(stepHeight)
                    }
                    shouldDisable = shouldDisable || disabled
                }
            }
        }

        safeListener<PlayerMoveEvent.Post> {
            if (globalCheck()) return@safeListener

            player.stepHeight = DEFAULT_HEIGHT
            if (shouldDisable) disable()
        }
    }

    private fun SafeClientEvent.shouldRunStep(x: Double, y: Double, z: Double): Boolean {
        return !mc.gameSettings.keyBindSneak.isKeyDown
            && !player.isFlying
            && !player.isOnLadder
            && !player.isInOrAboveLiquid
            && (mode == Mode.VANILLA || TimerManager.globalTicks > timeoutTick)
            && (!strictYMotion || y in -0.08..0.0 && player.lastTickPosY == player.posY)
            && (MovementUtils.isInputting() || HolePathFinder.isActive())
            && (x.sq + z.sq) > 0.001
    }

    private fun SafeClientEvent.vanillaStep() {
        player.stepHeight = maxHeight
        StepEvent.post()
        shouldDisable = autoDisable
    }

    private fun SafeClientEvent.packetStep(stepHeight: Double) {
        val array = getStepArray(stepHeight)
        if (array != null) {
            for (offset in array) {
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + offset, player.posZ, false))
            }

            player.stepHeight = maxHeight
            StepEvent.post()
            shouldDisable = autoDisable

            if (useTimer) {
                val targetTimer = 1.09f
                val extraPackets = array.size + 1
                val ticks = min((extraPackets / (targetTimer - postTimer)).ceilToInt(), maxPostTicks)
                val adjustedTimer = max((targetTimer * ticks - extraPackets) / ticks, postTimer)
                timeoutTick = TimerManager.globalTicks + ticks - 1
                modifyTimer(50.0f / adjustedTimer, ticks)
            }
        }
    }

    private fun SafeClientEvent.calcStepHeight(event: PlayerMoveEvent.Pre): Double {
        var playerBox = player.entityBoundingBox
        var motionX = event.x
        var motionY = event.y
        var motionZ = event.z

        var x1 = motionX
        val y1 = motionY
        var z1 = motionZ

        while (motionX != 0.0 && world.getCollisionBoxes(player, playerBox.offset(motionX, -maxHeight.toDouble(), 0.0))
                .isEmpty()
        ) {
            if (motionX < 0.05 && motionX >= -0.05) {
                motionX = 0.0
            } else if (motionX > 0.0) {
                motionX -= 0.05
            } else {
                motionX += 0.05
            }
            x1 = motionX
        }

        while (motionZ != 0.0 && world.getCollisionBoxes(player, playerBox.offset(0.0, -maxHeight.toDouble(), motionZ))
                .isEmpty()
        ) {
            if (motionZ < 0.05 && motionZ >= -0.05) {
                motionZ = 0.0
            } else if (motionZ > 0.0) {
                motionZ -= 0.05
            } else {
                motionZ += 0.05
            }
            z1 = motionZ
        }

        while (motionX != 0.0 && motionZ != 0.0 && world.getCollisionBoxes(
                player,
                playerBox.offset(motionX, -maxHeight.toDouble(), motionZ)
            ).isEmpty()
        ) {
            if (motionX < 0.05 && motionX >= -0.05) {
                motionX = 0.0
            } else if (motionX > 0.0) {
                motionX -= 0.05
            } else {
                motionX += 0.05
            }
            x1 = motionX
            if (motionZ < 0.05 && motionZ >= -0.05) {
                motionZ = 0.0
            } else if (motionZ > 0.0) {
                motionZ -= 0.05
            } else {
                motionZ += 0.05
            }
            z1 = motionZ
        }

        val list1 = world.getCollisionBoxes(player, playerBox.expand(motionX, motionY, motionZ))
        val axisalignedbb = playerBox

        if (motionY != 0.0) {
            for (box in list1) {
                motionY = box.calculateYOffset(playerBox, motionY)
            }
            playerBox = playerBox.offset(0.0, motionY, 0.0)
        }

        if (motionX != 0.0) {
            for (box in list1) {
                motionX = box.calculateXOffset(playerBox, motionX)
            }
            if (motionX != 0.0) {
                playerBox = playerBox.offset(motionX, 0.0, 0.0)
            }
        }

        if (motionZ != 0.0) {
            for (box in list1) {
                motionZ = box.calculateZOffset(playerBox, motionZ)
            }
//            if (motionZ != 0.0) {
//                playerBox = playerBox.offset(0.0, 0.0, motionZ)
//            }
        }

        val flag = player.onGround || y1 != motionY && y1 < 0.0

        if (flag && (x1 != motionX || z1 != motionZ)) {
            val x2 = motionX
//            val y2 = motionY
            val z2 = motionZ
            playerBox = axisalignedbb
            motionY = maxHeight.toDouble()
            val list = world.getCollisionBoxes(player, playerBox.expand(x1, motionY, z1))
            var axisalignedbb2 = playerBox
            val axisalignedbb3 = axisalignedbb2.expand(x1, 0.0, z1)
            var y3 = motionY

            for (box in list) {
                y3 = box.calculateYOffset(axisalignedbb3, y3)
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, y3, 0.0)
            var x3 = x1

            for (box in list) {
                x3 = box.calculateXOffset(axisalignedbb2, x3)
            }

            axisalignedbb2 = axisalignedbb2.offset(x3, 0.0, 0.0)
            var z3 = z1

            for (box in list) {
                z3 = box.calculateZOffset(axisalignedbb2, z3)
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, 0.0, z3)
            var axisalignedbb4 = playerBox
            var y4 = motionY

            for (box in list) {
                y4 = box.calculateYOffset(axisalignedbb4, y4)
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, y4, 0.0)
            var x4 = x1

            for (box in list) {
                x4 = box.calculateXOffset(axisalignedbb4, x4)
            }

            axisalignedbb4 = axisalignedbb4.offset(x4, 0.0, 0.0)
            var z4 = z1

            for (box in list) {
                z4 = box.calculateZOffset(axisalignedbb4, z4)
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, z4)
            val speed3 = x3.sq + z3.sq
            val speed4 = x4.sq + z4.sq

            if (speed3 > speed4) {
                motionX = x3
                motionZ = z3
                motionY = -y3
                playerBox = axisalignedbb2
            } else {
                motionX = x4
                motionZ = z4
                motionY = -y4
                playerBox = axisalignedbb4
            }

            for (box in list) {
                motionY = box.calculateYOffset(playerBox, motionY)
            }

            playerBox = playerBox.offset(0.0, motionY, 0.0)

            if (x2.sq + z2.sq < motionX.sq + motionZ.sq) {
                return playerBox.minY - player.posY
            }
        }

        return 0.0
    }

    private fun getStepArray(stepHeight: Double): DoubleArray? {
        return when (stepHeight) {
            in 0.6..1.05 -> {
                step10[0] = 0.42499 * stepHeight
                step10[1] = 0.75752 * stepHeight
                step10
            }
            in 1.05..1.2 -> step12
            in 1.2..1.3 -> step13
            in 1.3..1.5 -> step15
            in 1.5..2.0 -> step20
            in 2.0..2.5 -> step25
            else -> null
        }
    }

    private val step10 = doubleArrayOf(0.42499, 0.75752)
    private val step12 = doubleArrayOf(0.42499, 0.82721, 1.13981)
    private val step13 = doubleArrayOf(0.42499, 0.82108, 1.13367, 1.32728)
    private val step15 = doubleArrayOf(0.42499, 0.76, 1.01, 1.093, 1.015)
    private val step20 = doubleArrayOf(0.42499, 0.78, 0.63, 0.51, 0.90, 1.21, 1.45, 1.43)
    private val step25 = doubleArrayOf(0.42499, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907)

    fun isValidHeight(height: Double): Boolean {
        return height in minHeight..maxHeight
    }

    private fun globalCheck(): Boolean {
        return PacketFly.isActive()
    }
}