package dev.luna5ama.trollhack.graphics.blaze3d

import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector4f

object WorldProjection {
    private data class State(
        val projectionMatrix: Matrix4f,
        val viewMatrix: Matrix4f,
        val viewProjectionMatrix: Matrix4f,
        val worldToClipMatrix: Matrix4f,
        val cameraPosition: Vec3,
        val valid: Boolean,
    )

    data class Snapshot(
        val projectionMatrix: Matrix4f,
        val viewMatrix: Matrix4f,
        val worldToClipMatrix: Matrix4f,
        val cameraPosition: Vec3,
    )

    data class ScreenPosition(
        val x: Float,
        val y: Float,
        val depth: Float,
    )

    @Volatile
    private var state = State(
        Matrix4f(),
        Matrix4f(),
        Matrix4f(),
        Matrix4f(),
        Vec3.ZERO,
        false,
    )

    val projectionMatrix: Matrix4f
        get() = Matrix4f(state.projectionMatrix)

    val viewMatrix: Matrix4f
        get() = Matrix4f(state.viewMatrix)

    val worldToClipMatrix: Matrix4f
        get() = Matrix4f(state.worldToClipMatrix)

    val cameraPosition: Vec3
        get() = state.cameraPosition

    @JvmStatic
    fun capture(projectionMatrix: Matrix4fc, viewMatrix: Matrix4fc, cameraPosition: Vec3) {
        val projection = Matrix4f(projectionMatrix)
        val view = Matrix4f(viewMatrix)
        val viewProjection = Matrix4f(projection).mul(view)
        val worldToClip = Matrix4f(viewProjection)
            .translate(
                -cameraPosition.x.toFloat(),
                -cameraPosition.y.toFloat(),
                -cameraPosition.z.toFloat(),
            )

        state = State(
            projection,
            view,
            viewProjection,
            worldToClip,
            cameraPosition,
            projection.isFiniteMatrix() &&
                view.isFiniteMatrix() &&
                viewProjection.isFiniteMatrix() &&
                cameraPosition.x.isFinite() &&
                cameraPosition.y.isFinite() &&
                cameraPosition.z.isFinite(),
        )
    }

    /**
     * Projects an absolute world position into the logical coordinate space used by the current GUI canvas.
     */
    @JvmStatic
    fun worldToScreen(
        position: Vec3,
        framebufferWidth: Int,
        framebufferHeight: Int,
        logicalWidth: Float,
        logicalHeight: Float,
    ): ScreenPosition? {
        if (framebufferWidth <= 0 || framebufferHeight <= 0 ||
            !logicalWidth.isFinite() || !logicalHeight.isFinite() ||
            logicalWidth <= 0.0f || logicalHeight <= 0.0f ||
            !position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()
        ) {
            return null
        }

        val current = state
        if (!current.valid) return null

        // Keeping the input camera-relative avoids losing precision at large world coordinates.
        val clip = Vector4f(
            (position.x - current.cameraPosition.x).toFloat(),
            (position.y - current.cameraPosition.y).toFloat(),
            (position.z - current.cameraPosition.z).toFloat(),
            1.0f,
        )
        current.viewProjectionMatrix.transform(clip)

        if (!clip.x.isFinite() || !clip.y.isFinite() || !clip.z.isFinite() ||
            !clip.w.isFinite() || clip.w <= MIN_CLIP_W
        ) {
            return null
        }

        val inverseW = 1.0f / clip.w
        val ndcX = clip.x * inverseW
        val ndcY = clip.y * inverseW
        val ndcZ = clip.z * inverseW
        if (!ndcX.isFinite() || !ndcY.isFinite() || !ndcZ.isFinite() ||
            ndcX !in -1.0f..1.0f || ndcY !in -1.0f..1.0f || ndcZ !in -1.0f..1.0f
        ) {
            return null
        }

        val framebufferX = (ndcX + 1.0f) * 0.5f * framebufferWidth
        val framebufferY = (1.0f - ndcY) * 0.5f * framebufferHeight
        return ScreenPosition(
            framebufferX * logicalWidth / framebufferWidth,
            framebufferY * logicalHeight / framebufferHeight,
            (ndcZ + 1.0f) * 0.5f,
        )
    }

    fun snapshot(): Snapshot {
        val current = state
        return Snapshot(
            Matrix4f(current.projectionMatrix),
            Matrix4f(current.viewMatrix),
            Matrix4f(current.worldToClipMatrix),
            current.cameraPosition,
        )
    }

    private fun Matrix4fc.isFiniteMatrix(): Boolean =
        m00().isFinite() && m01().isFinite() && m02().isFinite() && m03().isFinite() &&
            m10().isFinite() && m11().isFinite() && m12().isFinite() && m13().isFinite() &&
            m20().isFinite() && m21().isFinite() && m22().isFinite() && m23().isFinite() &&
            m30().isFinite() && m31().isFinite() && m32().isFinite() && m33().isFinite()

    private const val MIN_CLIP_W = 1.0e-5f
}
