@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package cum.xiaro.trollhack.util.math.vector

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.util.extension.sq
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.hypot
import kotlin.math.sqrt

inline fun Entity.distanceTo(chunkPos: ChunkPos): Double {
    return distance(this.posX, this.posZ, (chunkPos.x * 16 + 8).toDouble(), (chunkPos.z * 16 + 8).toDouble())
}

inline fun Entity.distanceSqToBlock(chunkPos: ChunkPos): Int {
    return distanceSq(this.posX.fastFloor(), this.posZ.fastFloor(), chunkPos.x * 16 + 8, chunkPos.z * 16 + 8)
}

inline fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return hypot(x2 - x1, y2 - y1)
}

inline fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}

inline fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}


inline fun distanceSq(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq
}

inline fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq
}

inline fun distanceSq(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq
}


inline fun Vec3i.distanceTo(vec3d: Vec3d): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Vec3i.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}

inline fun Vec3i.distanceTo(vec3i: Vec3i): Double {
    return distance(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}

inline fun Vec3i.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

inline fun Vec3i.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Vec3i.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}

inline fun Vec3i.distanceSqTo(vec3i: Vec3i): Int {
    return distanceSq(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}

inline fun Vec3i.distanceSqTo(x: Int, y: Int, z: Int): Int {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}


inline fun Vec3d.distanceTo(vec3d: Vec3d): Double {
    return distance(this.x, this.y, this.z, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Vec3d.distanceTo(vec3i: Vec3i): Double {
    return distance(this.x, this.y, this.z, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

inline fun Vec3d.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

inline fun Vec3d.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}

inline fun Vec3d.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.x, this.y, this.z, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Vec3d.distanceSqTo(vec3i: Vec3i): Double {
    return distanceSq(this.x, this.y, this.z, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

inline fun Vec3d.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}

inline fun Vec3d.distanceSqTo(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}


inline fun Entity.distanceTo(vec3d: Vec3d): Double {
    return distance(this.posX, this.posY, this.posZ, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Entity.distanceTo(vec3i: Vec3i): Double {
    return distance(this.posX, this.posY, this.posZ, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

inline fun Entity.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.posX, this.posY, this.posZ, x, y, z)
}

inline fun Entity.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.posX, this.posY, this.posZ, x + 0.5, y + 0.5, z + 0.5)
}

inline fun Entity.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.posX, this.posY, this.posZ, vec3d.x, vec3d.y, vec3d.z)
}

inline fun Entity.distanceSqTo(vec3i: Vec3i): Double {
    return distanceSq(this.posX, this.posY, this.posZ, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

inline fun Entity.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.posX, this.posY, this.posZ, x, y, z)
}

inline fun Entity.distanceSqTo(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.posX, this.posY, this.posZ, x + 0.5, y + 0.5, z + 0.5)
}


inline fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
}

inline fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}

inline fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}


inline fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

inline fun distanceSq(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

inline fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}