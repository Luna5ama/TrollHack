package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object Bypass : Module(
    name = "Bypass",
    category = Category.CLIENT,
    description = "Configures bypass for anticheats",
    visible = false,
    alwaysEnabled = true
) {
    val placeRotationBoundingBoxGrow by setting("Place Rotation Bounding Box Grow", 0.1, 0.0..1.0, 0.01)
}