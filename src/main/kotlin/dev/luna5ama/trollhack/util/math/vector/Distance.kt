@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package dev.luna5ama.trollhack.util.math.vector

import dev.luna5ama.trollhack.util.extension.fastFloor
import dev.luna5ama.trollhack.util.extension.sq
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.hypot
import kotlin.math.sqrt

fun Entity.distanceTo(chunkPos: ChunkPos): Double {
    return distance(this.posX, this.posZ, (chunkPos.x * 16 + 8).toDouble(), (chunkPos.z * 16 + 8).toDouble())
}

fun Entity.distanceSqToBlock(chunkPos: ChunkPos): Int {
    return distanceSq(this.posX.fastFloor(), this.posZ.fastFloor(), chunkPos.x * 16 + 8, chunkPos.z * 16 + 8)
}

fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return hypot(x2 - x1, y2 - y1)
}

fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}

fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}


fun distanceSq(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq
}

fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq
}

fun distanceSq(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq
}


fun Vec3i.distanceTo(vec3d: Vec3d): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3i.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}

fun Vec3i.distanceTo(vec3i: Vec3i): Double {
    return distance(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}

fun Vec3i.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

fun Vec3i.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3i.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}

fun Vec3i.distanceSqTo(vec3i: Vec3i): Int {
    return distanceSq(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}

fun Vec3i.distanceSqTo(x: Int, y: Int, z: Int): Int {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}


fun Vec3d.distanceTo(vec3d: Vec3d): Double {
    return distance(this.x, this.y, this.z, vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3d.distanceTo(vec3i: Vec3i): Double {
    return distance(this.x, this.y, this.z, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

fun Vec3d.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

fun Vec3d.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}

fun Vec3d.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.x, this.y, this.z, vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3d.distanceSqTo(vec3i: Vec3i): Double {
    return distanceSq(this.x, this.y, this.z, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

fun Vec3d.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}

fun Vec3d.distanceSqTo(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}


fun Entity.distanceTo(vec3d: Vec3d): Double {
    return distance(this.posX, this.posY, this.posZ, vec3d.x, vec3d.y, vec3d.z)
}

fun Entity.distanceTo(vec3i: Vec3i): Double {
    return distance(this.posX, this.posY, this.posZ, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

fun Entity.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.posX, this.posY, this.posZ, x, y, z)
}

fun Entity.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.posX, this.posY, this.posZ, x + 0.5, y + 0.5, z + 0.5)
}

fun Entity.distanceSqTo(vec3d: Vec3d): Double {
    return distanceSq(this.posX, this.posY, this.posZ, vec3d.x, vec3d.y, vec3d.z)
}

fun Entity.distanceSqTo(vec3i: Vec3i): Double {
    return distanceSq(this.posX, this.posY, this.posZ, vec3i.x + 0.5, vec3i.y + 0.5, vec3i.z + 0.5)
}

fun Entity.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.posX, this.posY, this.posZ, x, y, z)
}

fun Entity.distanceSqTo(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.posX, this.posY, this.posZ, x + 0.5, y + 0.5, z + 0.5)
}


fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
}

fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}

fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}


fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

fun distanceSq(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}