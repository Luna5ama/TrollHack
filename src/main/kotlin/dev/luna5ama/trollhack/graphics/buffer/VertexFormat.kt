package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.trollhack.graphics.GLDataType

open class VertexFormat(vararg val elements: Element) {

    data object Pos2f : VertexFormat(Element.Position2f)
    data object Pos3fColorTex : VertexFormat(Element.Position3f, Element.Color, Element.Texture) // 24

    private val totalLength: Int

    init {
        var count = 0
        elements.forEach { count += it.length }
        totalLength = count
    }

    val attribute = buildAttribute(totalLength) {
        var index = 0
        elements.forEach {
            when (it.category) {
                Element.Category.Position -> {
                    float(index, it.count, it.glDataType, false)
                }

                Element.Category.Color -> {
                    float(index, it.count, it.glDataType, true)
                }

                Element.Category.Texture -> {
                    float(index, it.count, it.glDataType, false)
                }

                Element.Category.Index -> {
                    int(index, it.count, it.glDataType)
                }
            }
            index++
        }
    }

    enum class Element(val category: Category, val glDataType: GLDataType, val count: Int, val length: Int) {
        Position2f(Category.Position, GLDataType.GL_FLOAT, 2, 8),
        Position3f(Category.Position, GLDataType.GL_FLOAT, 3, 12),
        Position2d(Category.Position, GLDataType.GL_DOUBLE, 2, 16),
        Position3d(Category.Position, GLDataType.GL_DOUBLE, 3, 24),
        Position2i(Category.Position, GLDataType.GL_INT, 2, 8),
        Position3i(Category.Position, GLDataType.GL_INT, 3, 12),
        Color(Category.Color, GLDataType.GL_UNSIGNED_BYTE, 4, 4),
        Texture(Category.Texture, GLDataType.GL_FLOAT, 2, 8),
        TextureArray(Category.Texture, GLDataType.GL_FLOAT, 3, 12),
        LayerIndex(Category.Index, GLDataType.GL_INT, 1, 4);

        enum class Category {
            Position,
            Color,
            Texture,
            Index
        }
    }

}
