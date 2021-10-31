package cum.xiaro.trollhack.util.math.vector

import cum.xiaro.trollhack.util.math.MathUtils
import net.minecraft.util.math.Vec3d

fun Vec3d.lerp(to: Vec3d, delta: Float): Vec3d {
    val x = MathUtils.lerp(this.x, to.x, delta.toDouble())
    val y = MathUtils.lerp(this.y, to.y, delta.toDouble())
    val z = MathUtils.lerp(this.z, to.z, delta.toDouble())

    return Vec3d(x, y, z)
}

fun Vec3d.lerp(to: Vec3d, delta: Double): Vec3d {
    val x = MathUtils.lerp(this.x, to.x, delta)
    val y = MathUtils.lerp(this.y, to.y, delta)
    val z = MathUtils.lerp(this.z, to.z, delta)

    return Vec3d(x, y, z)
}