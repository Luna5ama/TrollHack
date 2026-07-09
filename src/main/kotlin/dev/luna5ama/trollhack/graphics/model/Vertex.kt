package dev.luna5ama.trollhack.graphics.model

import dev.luna5ama.kmogus.struct.Struct
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f

/**
 * Size = (3+3+2) * 4 = 32bytes
 */
@Struct
data class Vertex(
    val position: Vec3f,
    val normal: Vec3f,
    val texCoords: Vec2f,
    val tangent:Vec3f,
    val bitangent:Vec3f
)