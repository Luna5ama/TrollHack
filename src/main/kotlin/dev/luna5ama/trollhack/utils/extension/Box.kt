package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.toViewVec
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Check if a box is in sight
 */
fun AABB.isInSight(
    posFrom: Vec3 = PlayerPacketManager.position,
    rotation: Vec2f = RotationManager.rotation,
    range: Double = 8.0
): Boolean {
    return isInSight(posFrom, rotation.toViewVec(), range)
}

/**
 * Check if a box is in sight
 */
fun AABB.isInSight(
    posFrom: Vec3,
    viewVec: Vec3,
    range: Double = 4.25
): Boolean {
    val sightEnd = posFrom.add(viewVec.scale(range))
    return this.inflate(ClientSettings.placeRotationBoundingBoxGrow).intersects(posFrom, sightEnd)
}
