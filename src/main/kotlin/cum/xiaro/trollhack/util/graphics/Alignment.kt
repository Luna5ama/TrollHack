package cum.xiaro.trollhack.util.graphics

import cum.xiaro.trollhack.util.interfaces.DisplayEnum

enum class HAlign(override val displayName: CharSequence, val multiplier: Float, val offset: Float) : DisplayEnum {
    LEFT("Left", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    RIGHT("Right", 1.0f, 1.0f)
}

enum class VAlign(override val displayName: CharSequence, val multiplier: Float, val offset: Float) : DisplayEnum {
    TOP("Top", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    BOTTOM("Bottom", 1.0f, 1.0f)
}