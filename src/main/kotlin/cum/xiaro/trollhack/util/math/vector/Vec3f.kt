package cum.xiaro.trollhack.util.math.vector

data class Vec3f(val x: Float, val y: Float, val z: Float) {
    companion object {
        @JvmField
        val ZERO = Vec3f(0.0f, 0.0f, 0.0f)
    }
}