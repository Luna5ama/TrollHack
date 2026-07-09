package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.render.CoreRender2DEvent
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.font.UnicodeFontRenderer
import dev.luna5ama.trollhack.utils.runSafe

object RefreshFontCache : Module("Refresh FontCache", category = Category.CLIENT, alwaysListening = true) {
    private val showAtlas by setting("Show Atlas", false)
    private val atlasIndex by setting("Atlas Index", 0, { showAtlas })
    private val containChar by setting("Contain Char", "a")
    private val showStaticString by setting("Show Static String", false)

    init {
        nonNullHandler<TickEvent.Post> {
            disable()
        }

        nonNullHandler<CoreRender2DEvent> {
            if (showAtlas) {
                UnicodeFontManager.CURRENT_FONT.drawAtlas(atlasIndex)
            }
        }

        onEnabled {
            runSafe {
                UnicodeFontRenderer.refresh()
                disable()
            } ?: disable()
            disable()
        }
    }
}