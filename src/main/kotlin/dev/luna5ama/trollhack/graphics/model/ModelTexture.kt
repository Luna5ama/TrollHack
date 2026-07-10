package dev.luna5ama.trollhack.graphics.model

import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.graphics.texture.Texture

data class ModelTexture(
    val texture: Texture,
    val type: Type = Type.DIFFUSE,
    val name: String,
) : Texture by texture {
    enum class Type(override val displayName: String) : Displayable {
        DIFFUSE("texture_diffuse"),
        NORMAL("texture_normal"),
        SPECULAR("texture_specular"),
        HEIGHT("texture_height")
    }
}
