package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.combat.HolePathFinder
import cum.xiaro.trollhack.util.BaritoneUtils
import cum.xiaro.trollhack.util.EntityUtils.isFlying
import cum.xiaro.trollhack.util.EntityUtils.isInOrAboveLiquid
import cum.xiaro.trollhack.util.MovementUtils
import cum.xiaro.trollhack.util.MovementUtils.speed
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.network.play.client.CPacketPlayer

@Suppress("NOTHING_TO_INLINE")
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
    private val maxHeight by setting("Max Height", 2.0f, 0.6f..2.5f, 0.1f)
    private val enableTicks by setting("Enable Ticks", 10, 0..50, 1)

    const val DEFAULT_HEIGHT = 0.6f
    private var timeoutTick = -1
    private var shouldDisable = false
    private var prevCollided = false
    private var collideTicks = 0

    @Suppress("UNUSED")
    private enum class Mode {
        VANILLA, PACKET
    }

    override fun isActive(): Boolean {
        return isEnabled && runSafe { shouldRunStep() } ?: false
    }

    init {
        onDisable {
            mc.player?.apply {
                ridingEntity?.stepHeight = 1.0f
                stepHeight = DEFAULT_HEIGHT
            }
            timeoutTick = -1
            shouldDisable = false
            prevCollided = false
            collideTicks = 0
        }

        onToggle {
            BaritoneUtils.settings?.assumeStep?.value = it
        }

        safeListener<PlayerMoveEvent.Pre>(-100, true) {
            val flag = shouldRunStep()

            if (isDisabled) {
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
                val stepHeight = calcStepHeight()
                if (!stepHeight.isNaN() && stepHeight >= minHeight && stepHeight <= maxHeight) {
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
            player.stepHeight = DEFAULT_HEIGHT
            if (shouldDisable) disable()
        }
    }

    private inline fun SafeClientEvent.shouldRunStep(): Boolean {
        return !mc.gameSettings.keyBindSneak.isKeyDown
            && !player.isFlying
            && !player.isOnLadder
            && !player.isInOrAboveLiquid
            && (mode == Mode.VANILLA || player.ticksExisted > timeoutTick)
            && (!strictYMotion || player.motionY in -0.08..0.0 && player.lastTickPosY == player.posY)
            && (MovementUtils.isInputting || HolePathFinder.isActive())
            && player.speed > 0.09
    }

    private inline fun SafeClientEvent.vanillaStep() {
        player.stepHeight = maxHeight
        Speed.resetStep()
        shouldDisable = autoDisable
    }

    private inline fun SafeClientEvent.packetStep(stepHeight: Double) {
        val array = getStepArray(stepHeight)
        if (array != null) {
            for (offset in array) {
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + offset, player.posZ, false))
            }

            player.stepHeight = maxHeight
            Speed.resetStep()
            shouldDisable = autoDisable

            if (useTimer) {
                val timer = -0.0666f * array.size + 0.8333f
                val ticks = -(timer * (array.size + 1) / (timer - 1.0f)).fastCeil()
                timeoutTick = player.ticksExisted + ticks - 1
                modifyTimer(50.0f / timer, ticks)
            }
        }
    }

    private inline fun SafeClientEvent.calcStepHeight(): Double {
        var playerBox = player.entityBoundingBox
        var motionX = player.motionX
        var motionY = player.motionY
        var motionZ = player.motionZ

        var x1 = motionX
        val y1 = motionY
        var z1 = motionZ

        while (motionX != 0.0 && world.getCollisionBoxes(player, playerBox.offset(motionX, -maxHeight.toDouble(), 0.0)).isEmpty()) {
            if (motionX < 0.05 && motionX >= -0.05) {
                motionX = 0.0
            } else if (motionX > 0.0) {
                motionX -= 0.05
            } else {
                motionX += 0.05
            }
            x1 = motionX
        }

        while (motionZ != 0.0 && world.getCollisionBoxes(player, playerBox.offset(0.0, -maxHeight.toDouble(), motionZ)).isEmpty()) {
            if (motionZ < 0.05 && motionZ >= -0.05) {
                motionZ = 0.0
            } else if (motionZ > 0.0) {
                motionZ -= 0.05
            } else {
                motionZ += 0.05
            }
            z1 = motionZ
        }

        while (motionX != 0.0 && motionZ != 0.0 && world.getCollisionBoxes(player, playerBox.offset(motionX, -maxHeight.toDouble(), motionZ)).isEmpty()) {
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
            if (motionZ != 0.0) {
                playerBox = playerBox.offset(0.0, 0.0, motionZ)
            }
        }

        val flag = player.onGround || y1 != motionY && y1 < 0.0

        if (flag && (x1 != motionX || z1 != motionZ)) {
            val x2 = motionX
            val y2 = motionY
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

    private inline fun getStepArray(stepHeight: Double) =
        when (stepHeight) {
            in 0.6..1.0 -> stepOne
            in 1.0..1.5 -> stepOneHalf
            in 1.5..2.0 -> stepTwo
            in 2.0..2.5 -> stepTwoHalf
            else -> null
        }

    private val stepOne = doubleArrayOf(0.41999, 0.75320)
    private val stepOneHalf = doubleArrayOf(0.41999, 0.75320, 1.00133, 1.16611, 1.24919, 1.17079)
    private val stepTwo = doubleArrayOf(0.42, 0.78, 0.63, 0.51, 0.90, 1.21, 1.45, 1.43)
    private val stepTwoHalf = doubleArrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907)

    inline fun isValidHeight(height: Double): Boolean {
        return height >= minHeight && height <= maxHeight
    }
}