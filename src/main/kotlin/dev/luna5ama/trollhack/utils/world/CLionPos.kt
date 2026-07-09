package dev.luna5ama.trollhack.utils.world

import dev.luna5ama.trollhack.utils.math.fastFloor
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

class CLionPos(x: Double, y: Double, z: Double) :
    BlockPos(x.fastFloor(), y.fastFloor(), z.fastFloor()) {
    constructor(x: Double, y: Double, z: Double, patch: Boolean) : this(x, y + (if (patch) 0.3 else 0.0), z)

    constructor(Vec3: Vec3) : this(Vec3.x, Vec3.y, Vec3.z)

    constructor(Vec3: Vec3, fix: Boolean) : this(Vec3.x, Vec3.y, Vec3.z, fix)
}