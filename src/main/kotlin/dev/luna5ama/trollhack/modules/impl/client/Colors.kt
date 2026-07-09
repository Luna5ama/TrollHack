/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.color.ColorRGBA

internal object Colors : Module("Colors", category = Category.CLIENT, description = "LOL") {
    var red by setting("Red", 120, 0..255)
    var green by setting("Green", 85, 0..255)
    var blue by setting("Blue", 200, 0..255)
    val bRed by setting("Background Red", 0, 0..255)
    val bGreen by setting("Background Green", 0, 0..255)
    val bBlue by setting("Background Blue", 0, 0..255)
    val bAlpha by setting("Background Alpha", 85, 0..255)
    val blur by setting("Blur", false)
    val unfocusedAlpha by setting("Unfocused Alpha", 60, 0..80)
    val hoveredAlpha by setting("Hovered Alpha", 100, 80..200)
    val enabledAlpha by setting("Enabled Alpha", 255, 200..255)
    val primary by setting("Primary Color", ColorRGBA(255, 140, 180, 220))
    var rainbow by setting("Rainbow", false)

    val color get() = ColorRGBA(red, green, blue)
//    val background = setting("Background", "Shadow" , listOf("Shadow","Blur", "Both", "None"))
//    val setting = setting("Setting", "Side",listOf("Rect", "Side", "None"))
}