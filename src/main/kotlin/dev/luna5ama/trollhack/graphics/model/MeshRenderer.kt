package dev.luna5ama.trollhack.graphics.model

import dev.luna5ama.trollhack.graphics.matrix.MatrixLayerStack
import dev.luna5ama.trollhack.graphics.shader.Shader

abstract class MeshRenderer {
    abstract val shader: Shader
    abstract fun MatrixLayerStack.MatrixScope.draw(mesh: Mesh)
}