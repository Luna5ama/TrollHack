package dev.luna5ama.trollhack.graphics.skia

class GuiProjection(initialScale: Float = 2.0f) {
    var scale = initialScale
        private set
    var framebufferWidth = 1
        private set
    var framebufferHeight = 1
        private set
    var width = 0.5f
        private set
    var height = 0.5f
        private set

    fun update(framebufferWidth: Int, framebufferHeight: Int, requestedScale: Float): GuiProjection {
        val safeWidth = framebufferWidth.coerceAtLeast(1)
        val safeHeight = framebufferHeight.coerceAtLeast(1)
        val safeScale = requestedScale.coerceIn(0.5f, 8.0f)
        if (this.framebufferWidth == safeWidth &&
            this.framebufferHeight == safeHeight &&
            scale == safeScale
        ) return this

        this.framebufferWidth = safeWidth
        this.framebufferHeight = safeHeight
        scale = safeScale
        width = safeWidth / safeScale
        height = safeHeight / safeScale
        return this
    }
}
