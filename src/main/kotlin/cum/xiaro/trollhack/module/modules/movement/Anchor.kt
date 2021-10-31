package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.combat.Burrow
import cum.xiaro.trollhack.module.modules.combat.HoleSnap
import cum.xiaro.trollhack.module.modules.combat.Surround
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.MovementUtils.isCentered
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.math.vector.toVec3d

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
            if (Burrow.isEnabled || Surround.isEnabled) return@safeListener

            val playerPos = player.betterPosition
            val isInHole = player.onGround && HoleManager.getHoleInfo(playerPos).isHole

            if (HoleSnap.isActive() || !pitchTrigger || player.rotationPitch > pitch) {
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