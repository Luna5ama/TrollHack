package dev.luna5ama.trollhack.graphics.model

import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.graphics.texture.Texture
import org.lwjgl.assimp.Assimp

data class ModelTexture(
    val texture: Texture,
    val type: Type = Type.DIFFUSE,
    val name: String,
) : Texture by texture {
    enum class Type(override val displayName: String, val type: Int) : Displayable {
        DIFFUSE("texture_diffuse", Assimp.aiTextureType_DIFFUSE),
        NORMAL("texture_normal", Assimp.aiTextureType_NORMALS),
        SPECULAR("texture_specular", Assimp.aiTextureType_SPECULAR),
        HEIGHT("texture_height", Assimp.aiTextureType_HEIGHT)
    }
}