package me.luna.trollhack.module.modules.movement

import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.combat.Burrow
import me.luna.trollhack.module.modules.combat.HolePathFinder
import me.luna.trollhack.module.modules.combat.HoleSnap
import me.luna.trollhack.module.modules.combat.Surround
import me.luna.trollhack.util.EntityUtils.betterPosition
import me.luna.trollhack.util.MovementUtils.isCentered
import me.luna.trollhack.util.atTrue
import me.luna.trollhack.util.math.vector.toVec3d

internal object Anchor : Module(
    name = "Anchor",
    description = "Stops your motion when you are above hole",
    category = Category.MOVEMENT
) {
    private val autoCenter by setting("Auto Center", true)
    private val stopYMotion by setting("Stop Y Motion", true)
    private val pitchTrigger0 = setting("Pitch Trigger", true)
    private val pitchTrigger by pitchTrigger0
    private val pitch by setting("Pitch", 75, 0..90, 1, pitchTrigger0.atTrue())
    private val yRange by setting("Y Range", 3, 1..5, 1)

    init {
        safeListener<PlayerMoveEvent.Pre>(-1000) { event ->
            if (Burrow.isEnabled || Surround.isEnabled || HoleSnap.isActive() || HolePathFinder.isActive()) return@safeListener

            val playerPos = player.betterPosition
            val isInHole = player.onGround && HoleManager.getHoleInfo(playerPos).isHole

            if (!pitchTrigger || player.rotationPitch > pitch) {
                // Stops XZ motion
                val hole = HoleManager.getHoleBelow(playerPos, yRange) {
                    it.canEnter(world, playerPos)
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