package dev.luna5ama.trollhack.modules.impl.visual

import com.mojang.blaze3d.opengl.GlStateManager
import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.getFloatArray
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.graphics.shader.useShader
import dev.luna5ama.trollhack.utils.math.vectors.Vec2i
import net.minecraft.world.entity.Entity
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*

object Shaders : Module("Shaders", category = Category.VISUAL) {

    fun shouldRender(entity: Entity): Boolean {
        return isEnabled && entity != MinecraftWrapper.mc.player
    }

    object OutlineShader : Shader("/assets/shader/effect/outline.vsh", "/assets/shader/effect/outline.fsh") {
        private var lastResolution = Vec2i.ZERO
        private val projMat = Matrix4f()

        fun updateUniform(resolution: Vec2i) {
            useShader {
//                if (resolution != lastResolution) {
//                    projMat.identity()
                    projMat.setOrtho(0f, resolution.x.toFloat(), 0f, resolution.y.toFloat(), 0.1f, 1000f)
//                }

//                println(Vector4f(resolution.x.toFloat(), resolution.y.toFloat(), 0f, 1f).mul(projMat))
                glUniformMatrix4fv(0, false, projMat.getFloatArray())
                glUniform2f(1, resolution.x.toFloat(), resolution.y.toFloat())
                glUniform2f(2, resolution.x.toFloat(), resolution.y.toFloat())
            }
        }
    }
}