package dev.luna5ama.trollhack.graphics.model

abstract class AbstractModel {
    protected val textures = mutableListOf<ModelTexture>()
    protected val meshes = mutableListOf<Mesh>()
}