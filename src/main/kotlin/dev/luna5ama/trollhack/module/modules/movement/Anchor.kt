package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.combat.HolePathFinder
import dev.luna5ama.trollhack.module.modules.combat.HoleSnap
import dev.luna5ama.trollhack.module.modules.combat.Surround
import dev.luna5ama.trollhack.module.modules.exploit.Burrow
import dev.luna5ama.trollhack.module.modules.exploit.WallClip
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.math.vector.toVec3d

internal object Anchor : Module(
    name = "Anchor",
    description = "Stops your motion when you are above hole",
    category = Category.MOVEMENT,
    modulePriority = 99999
) {
    private val autoCenter by setting("Auto Center", true)
    private val stopYMotion by setting("Stop Y Motion", true)
    private val pitchTrigger0 = setting("Pitch Trigger", true)
    private val pitchTrigger by pitchTrigger0
    private val pitch by setting("Pitch", 75, 0..90, 1, pitchTrigger0.atTrue())
    private val yRange by setting("Y Range", 3, 1..5, 1)
    private val fallTimer by setting("Fall Timer", 0.0f, 0.0f..2.0f, 0.01f)

    private var inactiveTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks < 2
    }

    init {
        safeListener<PlayerMoveEvent.Pre>(-1000) { event ->
            inactiveTicks++

            if (Burrow.isEnabled
                || WallClip.run { isEnabled || isClipped() }
                || Surround.isEnabled
                || HoleSnap.isActive()
                || HolePathFinder.isActive()
            ) return@safeListener

            val playerPos = player.betterPosition
            val isInHole = player.onGround && HoleManager.getHoleInfo(playerPos).isHole

            if (!pitchTrigger || player.rotationPitch > pitch) {
                // Stops XZ motion
                val hole = HoleManager.getHoleBelow(playerPos, yRange) {
                    it.canEnter(world, playerPos)
                }

                if (fallTimer != 0.0f && hole != null && !player.onGround) {
                    modifyTimer(50.0f / fallTimer)
                }

                if (isInHole || hole != null) {
                    val center = hole?.center ?: playerPos.toVec3d(0.5, 0.0, 0.5)

                    if (player.isCentered(center)) {
                        if (!player.isSneaking) {
                            player.motionX = 0.0
                            player.motionZ = 0.0
                            event.x = 0.0
                            event.z = 0.0
                        }
                    } else if (autoCenter) {
                        AutoCenter.centerPlayer(center)
                    }
                }

                // Stops Y motion
                if (stopYMotion && isInHole) {
                    player.motionY = -0.08
                    event.y = -0.08 // Minecraft needs this for on ground check
                }
            }
        }
    }
}