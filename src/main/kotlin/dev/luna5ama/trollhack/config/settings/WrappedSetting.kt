package dev.luna5ama.trollhack.config.settings

import kotlin.properties.ReadWriteProperty

interface WrappedSetting<T : Any> : ReadWriteProperty<Any?, T> {
    val delegate: AbstractSetting<String, *>
}