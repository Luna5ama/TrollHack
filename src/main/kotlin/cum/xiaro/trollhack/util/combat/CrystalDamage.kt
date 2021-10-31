package cum.xiaro.trollhack.util.combat

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class CrystalDamage(
    val crystalPos: Vec3d,
    val blockPos: BlockPos,
    val selfDamage: Float,
    val targetDamage: Float,
    val eyeDistance: Double,
    val feetDistance: Double
) {
    val damageBalance = targetDamage - selfDamage
}