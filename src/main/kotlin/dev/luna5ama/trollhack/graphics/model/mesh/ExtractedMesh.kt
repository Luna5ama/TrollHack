package dev.luna5ama.trollhack.graphics.model.mesh

import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.MatrixLayerStack
import dev.luna5ama.trollhack.graphics.model.Mesh
import dev.luna5ama.trollhack.graphics.model.MeshData
import dev.luna5ama.trollhack.graphics.model.MeshRenderer
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.graphics.shader.glUniform
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL31.GL_CLIP_DISTANCE0
import org.lwjgl.opengl.GL31.glDrawElementsInstanced

var lightPosition = Vec3f(45f, 45f, 45f)

class ExtractedMesh(meshData: MeshData) : Mesh(
    meshData.vertices,
    meshData.diffuse,
    meshData.normal,
    meshData.specular,
    meshData.height,
    meshData.indices,
    meshData.primitives
) {

    companion object DrawShader : MeshRenderer() {

        override val shader = Shader("/assets/shader/model/MeshVertex.vsh", "/assets/shader/model/MeshDNSH.fsh")
        private val matrixUniform = shader.getUniformLocation("matrix")
        private val diffuse = shader.getUniformLocation("diffuseTex")
        private val normal = shader.getUniformLocation("normalTex")
        private val specular = shader.getUniformLocation("specularTex")
        private val height = shader.getUniformLocation("heightTex")
        private val viewPos = shader.getUniformLocation("viewPos")
        private val viewRotation = shader.getUniformLocation("viewRotation")
        private val lightColor = shader.getUniformLocation("lightColor")
        private val lightPos = shader.getUniformLocation("lightPos")

        override fun MatrixLayerStack.MatrixScope.draw(mesh: Mesh) {
            val context = NonNullContext.instance ?: return
            if (mesh.textures.isEmpty()) return

//            val c = ColorRGBA.GOLD.mix(ColorRGBA.WHITE, 0.7f)
            val c = ColorRGBA.WHITE
            val color = Vec3f(c.rFloat, c.gFloat, c.bFloat)
            val pitch0 = (((System.currentTimeMillis() - 114514) % 5000) / 5000f * 360f).toRadian()
            lightPosition = Vec3f(pitch0, 10f.toRadian()) * 10f

            shader.bind()
            layer.matrixArray.glUniform(matrixUniform)

            // upload Lights
            color.glUniform(lightColor)
            lightPosition.glUniform(lightPos)

            val camera = context.mc.entityRenderDispatcher.camera!!
            camera.position().glUniform(viewPos)
            val yaw = camera.yRot()
            val pitch = camera.xRot()
            val rotation = RotationUtils.getDirectionVec(Vec2f(yaw, pitch))
//            println("${rotation.x} ${rotation.y} ${rotation.z} ${rotation.w}")
            glUniform3f(viewRotation, rotation.x, rotation.y, rotation.z)

            mesh.diffuseTexture?.let {
                glActiveTexture(GL_TEXTURE0)
                glUniform1i(diffuse, 0)
                it.bindTexture()
            }

            mesh.normalTexture?.let {
                glActiveTexture(GL_TEXTURE1)
                glUniform1i(normal, 1)
                it.bindTexture()
            }

            mesh.specularTexture?.let {
                glActiveTexture(GL_TEXTURE2)
                glUniform1i(specular, 2)
                it.bindTexture()
            }

            mesh.heightTexture?.let {
                glActiveTexture(GL_TEXTURE3)
                glUniform1i(height, 3)
                it.bindTexture()
            }

            GLHelper.cull = true
            GLHelper.depth = true
            // Draw mesh via VAO
            GLHelper.bindVertexArray(mesh.vao)
            glDrawElementsInstanced(GL_TRIANGLES, mesh.vertices.size, GL_UNSIGNED_INT, 0, 100 * 10)
            glActiveTexture(GL_TEXTURE0)
        }

    }

    init {
        setupMesh()
    }

    override fun MatrixLayerStack.MatrixScope.draw() = draw(this@ExtractedMesh)

}
