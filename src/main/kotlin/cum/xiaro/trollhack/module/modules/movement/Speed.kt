package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.extension.synchronized
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.events.player.PlayerTravelEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.manager.managers.TimerManager.resetTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.combat.HoleSnap
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.EntityUtils.isInOrAboveLiquid
import cum.xiaro.trollhack.util.MovementUtils.applyJumpBoostPotionEffects
import cum.xiaro.trollhack.util.MovementUtils.applySpeedPotionEffects
import cum.xiaro.trollhack.util.MovementUtils.calcMoveYaw
import cum.xiaro.trollhack.util.MovementUtils.speedEffectMultiplier
import cum.xiaro.trollhack.util.accessor.isInWeb
import cum.xiaro.trollhack.util.math.vector.Vec2d
import cum.xiaro.trollhack.util.math.vector.distanceSqTo
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.*

@Suppress("NOTHING_TO_INLINE")
internal object Speed : Module(
    name = "Speed",
    alias = arrayOf("Strafe"),
    category = Category.MOVEMENT,
    description = "Improves control in air",
    modulePriority = 100
) {
    private val page = setting("Page", Page.STRAFE)

    private val bbtt by setting("2B2T", false, page.atValue(Page.STRAFE))
    private val timerBoost by setting("Timer Boost", 1.09f, 1.0f..1.5f, 0.01f, page.atValue(Page.STRAFE))
    private val baseSpeed by setting("Base Speed", 0.2873, 0.0..0.5, 0.0001, page.atValue(Page.STRAFE))
    private val maxStepSpeed by setting("Max Step Speed", 0.35, 0.0..0.5, 0.01, page.atValue(Page.STRAFE))
    private val maxSpeed by setting("Max Speed", 1.0, 0.1..2.0, 0.01, page.atValue(Page.STRAFE))
    private val airDecay by setting("Air Decay", 0.9937, 0.0..1.0, 0.0001, page.atValue(Page.STRAFE))
    private val autoJump by setting("Auto Jump", true, page.atValue(Page.STRAFE))
    private val jumpMotion by setting("Jump Motion", 0.4, 0.1..0.5, 0.01, page.atValue(Page.STRAFE))
    private val jumpBoost by setting("Jump Boost", 2.0f, 1.0f..4.0f, 0.01f, page.atValue(Page.STRAFE))
    private val jumpDecay by setting("Jump Decay", 0.66f, 0.0f..1.0f, 0.01f, page.atValue(Page.STRAFE))
    private val maxJumpSpeed by setting("Max Jump Speed", 0.548, 0.1..2.0, 0.01, page.atValue(Page.STRAFE))
    private val jumpDelay by setting("Jump Delay", 5, 0..10, 1, page.atValue(Page.STRAFE) and ::autoJump)

    private val velocityBoost by setting("Velocity Boost", 0.5, 0.0..2.0, 0.01, page.atValue(Page.BOOST))
    private val minBoostSpeed by setting("Min Boost Speed", 0.2, 0.0..1.0, 0.01, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val maxBoostSpeed by setting("Max Boost Speed", 0.6, 0.0..1.0, 0.01, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val maxYSpeed by setting("Max Y Speed", 0.5, 0.0..1.0, 0.01, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val boostDelay by setting("Boost Delay", 250, 0..5000, 50, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val boostTimeout by setting("Boost Timeout", 500, 0..2500, 50, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val boostDecay by setting("Boost Decay", 0.98, 0.1..1.0, 0.001, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })
    private val boostRange by setting("Boost Range", 4.0f, 1.0f..8.0f, 0.1f, page.atValue(Page.BOOST) and { velocityBoost > 0.0 })

    private val wallCheck by setting("Wall Check", false, page.atValue(Page.MISC))
    private val stepPause by setting("Step Pause", 4, 0..20, 1, page.atValue(Page.MISC))
    private val reverseStepPause by setting("Reverse Step Pause", 0, 0..10, 1, page.atValue(Page.MISC))
    private val holeCheck0 = setting("Hole Check", true, page.atValue(Page.MISC))
    private val holeCheck by holeCheck0
    private val hRange by setting("H Range", 0.15f, 0.0f..2.0f, 0.05f, page.atValue(Page.MISC) and holeCheck0.atTrue())
    private val predictTicks by setting("Predict Ticks", 4, 0..10, 1, page.atValue(Page.MISC) and holeCheck0.atTrue())

    private enum class Page {
        STRAFE, BOOST, MISC
    }

    private val boostTimer = TickTimer()

    private var state = State.AIR
    private var moveSpeed = 0.0
    private var prevPos: Vec2d? = null
    private var lastDist = 0.0

    private var strafeTicks = 0x22
    private var burrowTicks = 0x22
    private var jumpTicks = 0x22
    private var stepTicks = 0x22
    private var reverseStepPauseTicks = 0x22
    private var stepPauseTicks = 0x22
    private var inHoleTicks = 0x22
    private var rubberBandTicks = 0x22

    private var prevCollided = false
    private var prevSpeed = 0.0

    private var boostingSpeed = 0.0
    private val boostList = ArrayList<BoostInfo>().synchronized()
    private val stepHeights = doubleArrayOf(0.605, 1.005, 1.505, 2.005, 2.505)

    private enum class State {
        JUMP, DECAY, AIR
    }

    init {
        onEnable {
            reset()
            resetBoost()

            strafeTicks = 0x22
            burrowTicks = 0x22
            jumpTicks = 0x22
            stepTicks = 0x22
            reverseStepPauseTicks = 0x22
            stepPauseTicks = 0x22
            inHoleTicks = 0x22
            rubberBandTicks = 0x22
        }

        safeListener<PacketEvent.Receive> {
            when (it.packet) {
                is SPacketPlayerPosLook -> {
                    rubberBandTicks = 0
                    reset()
                    resetBoost()
                    boostTimer.reset(1000L)
                }
                is SPacketExplosion -> {
                    handleVelocity(it, Vec3d(it.packet.x, it.packet.y, it.packet.z), hypot(it.packet.motionX.toDouble(), it.packet.motionZ.toDouble()), it.packet.motionY.toDouble())
                }
                is SPacketEntityVelocity -> {
                    if (it.packet.entityID == player.entityId) {
                        handleVelocity(it, null, hypot(it.packet.motionX / 8000.0, it.packet.motionZ / 8000.0) - moveSpeed, it.packet.motionY / 8000.0)
                    }
                }
            }
        }

        safeListener<TickEvent.Post> {
            strafeTicks++
            burrowTicks++
            jumpTicks++
            stepTicks++
            reverseStepPauseTicks++
            stepPauseTicks++
            inHoleTicks++
            rubberBandTicks++
        }

        safeListener<PlayerTravelEvent> {
            val playerPos = player.betterPosition
            val box = world.getBlockState(playerPos).getCollisionBoundingBox(world, playerPos)
            if (box != null && box.maxY + playerPos.y > player.posY) {
                burrowTicks = 0
            }

            if (!shouldStrafe()) {
                reset()
                return@safeListener
            }

            if (isBurrowed()) modifyTimer(50.0f / timerBoost)

            strafeTicks = 0
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (shouldStrafe()) {
                val yaw = calcMoveYaw()
                val motionX = -sin(yaw)
                val motionZ = cos(yaw)
                val baseSpeed = player.applySpeedPotionEffects(baseSpeed)

                updateState(baseSpeed, motionX, motionZ)
                updateFinalSpeed(baseSpeed)
                if (boostTimeout > 0) applyVelocityBoost()

                val boostedSpeed = min(moveSpeed + boostingSpeed, player.applySpeedPotionEffects(maxSpeed))
                player.motionX = motionX * baseSpeed
                player.motionZ = motionZ * baseSpeed
                it.x = motionX * boostedSpeed
                it.z = motionZ * boostedSpeed

                if (!prevCollided && !player.collidedHorizontally && jumpTicks > 2) {
                    prevSpeed = moveSpeed
                    stepTicks = 0
                }
                prevPos = Vec2d(player.posX, player.posZ)
                prevCollided = player.collidedHorizontally
            } else if (strafeTicks <= 1) {
                player.motionX = 0.0
                player.motionZ = 0.0
                it.x = 0.0
                it.z = 0.0
            }
        }

        safeListener<PlayerMoveEvent.Post> {
            if (jumpTicks == 0) {
                player.stepHeight = Step.DEFAULT_HEIGHT
            }

            prevPos?.let {
                lastDist = hypot(it.x - player.posX, it.y - player.posZ)
            }

            prevSpeed *= 0.9937106918238994
            updateVelocityBoost()
        }
    }

    private inline fun SafeClientEvent.handleVelocity(event: PacketEvent.Receive, pos: Vec3d?, speed: Double, motionY: Double) {
        if (velocityBoost == 0.0) return
        if (isBurrowed()) return
        if (abs(motionY) > maxYSpeed) return
        if (pos != null && player.distanceSqTo(pos) > boostRange.sq) return

        val newSpeed = min(speed * velocityBoost, maxBoostSpeed)

        if (newSpeed >= minBoostSpeed) {
            if (boostTimeout == 0) {
                if (!prevCollided && !player.collidedHorizontally) {
                    synchronized(boostList) {
                        if (newSpeed > boostingSpeed && boostTimer.tickAndReset(0L)) {
                            boostingSpeed = newSpeed
                            boostTimer.reset(boostDelay)
                            event.cancel()
                        }
                    }
                }
            } else {
                synchronized(boostList) {
                    boostList.add(BoostInfo(pos, newSpeed, System.currentTimeMillis() + boostTimeout))
                    event.cancel()
                }
            }
        }
    }

    private inline fun SafeClientEvent.shouldStrafe(): Boolean {
        return !player.capabilities.isFlying
            && !player.isElytraFlying
            && !mc.gameSettings.keyBindSneak.isKeyDown
            && !player.isInOrAboveLiquid
            && !player.isInWeb
            && !player.isOnLadder
            && Flight.isDisabled
            && MovementUtils.isInputting
            && !HoleSnap.isActive()
            && !BaritoneUtils.isPathing
    }

    private inline fun SafeClientEvent.updateState(baseSpeed: Double, motionX: Double, motionZ: Double) {
        if (player.onGround) {
            state = State.JUMP
        }

        when (state) {
            State.JUMP -> {
                if (player.onGround) {
                    if (autoJump) {
                        if (jumpTicks >= jumpDelay) {
                            smartJump(motionX, motionZ)
                            state = State.DECAY
                        }
                    } else if (abs(player.motionY - player.applyJumpBoostPotionEffects(0.42)) <= 0.01) {
                        jump()
                        state = State.DECAY
                    }
                }
            }
            State.DECAY -> {
                val decayFactor = if (bbtt) max(jumpDecay, 0.795f) else jumpDecay
                val jumpBoostDecay = decayFactor * (lastDist - baseSpeed)
                moveSpeed = lastDist - jumpBoostDecay
                state = State.AIR
            }
            State.AIR -> {
                var decayFactor = airDecay
                if (decayFactor == 0.9937) decayFactor = 0.9937106918238994
                moveSpeed = lastDist * decayFactor
            }
        }
    }

    private inline fun SafeClientEvent.smartJump(motionX: Double, motionZ: Double) {
        val dist = calcBlockDistAhead(motionX * 6.0, motionZ * 6.0)
        val stepHeight = calcStepHeight(dist, motionX, motionZ)
        val multiplier = player.speedEffectMultiplier

        if (wallCheck && dist < 3.0 * multiplier && stepHeight > 1.114514) return
        if (dist < 1.4 * multiplier && Step.isActive() && Step.isValidHeight(stepHeight)) return
        if (stepPauseTicks < stepPause || reverseStepPauseTicks < reverseStepPause) return
        if (!holeCheck(motionX * multiplier, motionZ * multiplier, dist)) return

        jump()
    }

    private inline fun SafeClientEvent.holeCheck(motionX: Double, motionZ: Double, dist: Double): Boolean {
        if (!holeCheck || inHoleTicks <= 20 || mc.gameSettings.keyBindJump.isKeyDown) return true

        val speed = moveSpeed * predictTicks
        val start = player.positionVector
        val end = start.add(motionX * speed, 0.0, motionZ * speed)

        for (holeInfo in HoleManager.holeInfos) {
            if (holeInfo.origin.y >= player.posY) continue
            if (hypot(holeInfo.center.x - player.posX, holeInfo.center.z - player.posZ) > dist) continue
            val box = holeInfo.boundingBox.toDetectBox(player.posY)
            if (box.contains(start)) return false
            if (predictTicks != 0 && (box.contains(end) || box.calculateIntercept(start, end) != null)) return false
        }

        return true
    }

    private inline fun AxisAlignedBB.toDetectBox(playerY: Double): AxisAlignedBB {
        return AxisAlignedBB(
            this.minX - hRange, this.minY, this.minZ - hRange,
            this.maxX + hRange, max(this.maxX, playerY), this.maxZ + hRange
        )
    }

    private inline fun SafeClientEvent.calcBlockDistAhead(offsetX: Double, offsetZ: Double): Double {
        if (player.collidedHorizontally) return 0.0

        val box = player.entityBoundingBox
        val x = if (offsetX > 0.0) box.maxX else box.minX
        val z = if (offsetX > 0.0) box.maxZ else box.minZ

        return min(
            world.rayTraceDist(Vec3d(x, box.minY + 0.6, z), offsetX, offsetZ),
            world.rayTraceDist(Vec3d(x, box.maxY + 0.6, z), offsetX, offsetZ)
        )
    }

    private inline fun WorldClient.rayTraceDist(start: Vec3d, offsetX: Double, offsetZ: Double): Double {
        return this.rayTraceBlocks(start, start.add(offsetX, 0.0, offsetZ), false, true, false)
            ?.hitVec?.let {
                val x = start.x - it.x
                val z = start.z - it.z
                sqrt(x.pow(2) + z.pow(2))
            } ?: 999.0
    }

    private inline fun SafeClientEvent.jump() {
        player.motionY = calcJumpMotion()
        player.isAirBorne = true
        player.stepHeight = 0.0f
        jumpTicks = 0

        if (boostingSpeed > 0.1 || rubberBandTicks <= 2 || boostList.isNotEmpty()) {
            moveSpeed *= 1.2
        } else {
            moveSpeed = min(moveSpeed * jumpBoost, player.applySpeedPotionEffects(maxJumpSpeed))
        }
    }

    private inline fun SafeClientEvent.calcJumpMotion(): Double {
        val motion = when {
            isBurrowed() -> 0.42
            jumpMotion == 0.4 -> 0.399399995803833
            else -> jumpMotion
        }

        return player.applyJumpBoostPotionEffects(motion)
    }

    private inline fun SafeClientEvent.updateFinalSpeed(baseSpeed: Double) {
        if (!isBurrowed()) {
            moveSpeed = max(moveSpeed, baseSpeed)

            if (prevCollided && stepTicks < 10 && rubberBandTicks <= 2) {
                val stepSpeed = min(prevSpeed, player.applySpeedPotionEffects(maxStepSpeed))
                moveSpeed = max(moveSpeed, stepSpeed)
                if (!player.collidedHorizontally) prevSpeed = 0.0
            }
        } else {
            moveSpeed = baseSpeed
        }

        moveSpeed = min(moveSpeed, player.applySpeedPotionEffects(maxSpeed))
    }

    private inline fun SafeClientEvent.applyVelocityBoost() {
        if (isBurrowed()) {
            resetBoost()
        } else {
            val removeTime = System.currentTimeMillis()
            boostList.removeIf {
                it.time < removeTime || it.speed < 0.1
            }

            if (jumpTicks != 0 && boostTimer.tick(boostDelay)) {
                val rangeSq = boostRange.sq
                synchronized(boostList) {
                    boostList.asSequence()
                        .filter { it.speed > boostingSpeed }
                        .filter { it.pos == null || player.distanceSqTo(it.pos) <= rangeSq }
                        .maxByOrNull { it.speed }
                        ?.let {
                            boostingSpeed = it.speed
                            boostTimer.reset(boostDelay)
                            resetBoost()
                        }
                }
            }
        }
    }

    private inline fun SafeClientEvent.updateVelocityBoost() {
        val decay = boostDecay * if (player.onGround) {
            val blockPos = BlockPos(player.posX, player.entityBoundingBox.minY - 1.0, player.posZ)
            val blockState = world.getBlockState(blockPos)
            blockState.block.getSlipperiness(blockState, world, blockPos, player) * 0.91
        } else {
            0.91
        }

        synchronized(boostList) {
            boostList.forEach {
                it.speed *= decay
            }
        }

        if (player.collidedHorizontally) {
            boostingSpeed = 0.0
        } else {
            boostingSpeed *= decay
        }
    }

    private inline fun SafeClientEvent.calcStepHeight(dist: Double, motionX: Double, motionZ: Double): Double {
        val pos = player.betterPosition
        if (world.getBlockState(pos).getCollisionBoundingBox(world, pos) != null) return 0.0

        val i = max(dist.roundToInt(), 1)
        var minStepHeight = Double.MAX_VALUE

        val x = motionX * i
        val z = motionZ * i
        minStepHeight = checkBox(minStepHeight, x, 0.0)
        minStepHeight = checkBox(minStepHeight, 0.0, z)

        return if (minStepHeight == Double.MAX_VALUE) 0.0 else minStepHeight
    }

    private inline fun SafeClientEvent.checkBox(minStepHeight: Double, offsetX: Double, offsetZ: Double): Double {
        val box = player.entityBoundingBox.offset(offsetX, 0.0, offsetZ)
        if (!world.collidesWithAnyBlock(box)) return minStepHeight

        var stepHeight = minStepHeight

        for (y in stepHeights) {
            if (y > minStepHeight) break

            val stepBox = AxisAlignedBB(
                box.minX, box.minY + y - 0.5, box.minZ,
                box.maxX, box.minY + y, box.maxZ
            )
            val boxList = world.getCollisionBoxes(null, stepBox)
            val maxHeight = boxList.maxOfOrNull { it.maxY } ?: continue
            val maxStepHeight = maxHeight - player.posY

            if (!world.collidesWithAnyBlock(box.offset(0.0, maxStepHeight, 0.0))) {
                stepHeight = maxStepHeight
                break
            }
        }

        return stepHeight
    }

    fun reset() {
        mc.player?.jumpMovementFactor = 0.02f
        resetTimer()

        state = State.AIR
        moveSpeed = mc.player?.applySpeedPotionEffects(baseSpeed) ?: baseSpeed
        prevPos = null
        lastDist = 0.0

        prevCollided = false
        prevSpeed = 0.0

        boostingSpeed = 0.0
    }

    private inline fun resetBoost() {
        boostList.clear()
    }

    private inline fun isBurrowed(): Boolean {
        return burrowTicks < 10
    }

    fun resetReverseStep() {
        stepPauseTicks = 0
    }

    fun resetStep() {
        stepPauseTicks = 0
        prevSpeed = 0.0
    }

    private class BoostInfo(
        val pos: Vec3d?,
        var speed: Double,
        val time: Long
    )
}
