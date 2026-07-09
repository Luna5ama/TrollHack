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

package dev.luna5ama.trollhack.modules

import org.lwjgl.glfw.GLFW

abstract class Module @JvmOverloads constructor(
    name: CharSequence,
    description: CharSequence = "",
    category: Category,
    hidden: Boolean = false,
    alwaysListening: Boolean = false,
    enableByDefault: Boolean = false,
    alwaysEnable: Boolean = false,
    defaultBind: Int = GLFW.GLFW_KEY_UNKNOWN,
    modulePriority: Int = 0,
    alias: Set<CharSequence> = setOf(name),
    internal: Boolean = false,
    excludedFromConfig: Boolean = false
) : AbstractModule(name, description, category, hidden, alwaysListening, enableByDefault,
    alwaysEnable, defaultBind, modulePriority, alias, internal, excludedFromConfig)