/*
 * Copyright (c) 2023-2024 TrollHack 保留所有权利�?All Right Reserved.
 */

package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module


object AspectRatio : Module("Aspect Ratio", "AspectRatio", Category.VISUAL) {
    @JvmStatic
    val ratio by setting("Ratio", 1.78f, 0.1f..8f)
}