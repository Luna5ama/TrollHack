package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

object NoCameraClip : Module(
    "Camera Clip",
    "NoCameraClip",
    Category.RENDER
) {
    /** Maximum third-person distance. The name is kept for config compatibility. */
    val distance by setting("Distance", 3.5f, 1.0f..20.0f, 0.5f)
    val action by setting("Action", true)
    val actionSmoothness by setting("Action Smoothness", 0.3f, 0.1f..0.95f, 0.01f)
    val actionMaxDistance by setting("Action Max Distance", 20.0f, 1.0f..50.0f, 0.5f)

    private var cameraPos: Vec3? = null

    init {
        onEnabled { cameraPos = null }
        onDisabled { cameraPos = null }
    }

    fun resetCameraPos() {
        cameraPos = null
    }

    fun updateActionCamera(targetPos: Vec3) {
        val current = cameraPos
        if (current == null) {
            cameraPos = targetPos
            return
        }

        val targetDistance = current.distanceTo(targetPos)
        val maxDistance = actionMaxDistance.toDouble()
        if (targetDistance > maxDistance) {
            cameraPos = targetPos
            return
        }

        val factor = actionSmoothness.toDouble() * (1.0 - kotlin.math.exp(-targetDistance / maxDistance))
        cameraPos = Vec3(
            Mth.lerp(factor, current.x, targetPos.x),
            Mth.lerp(factor, current.y, targetPos.y),
            Mth.lerp(factor, current.z, targetPos.z)
        )
    }

    fun cameraPos(): Vec3? = cameraPos
}
