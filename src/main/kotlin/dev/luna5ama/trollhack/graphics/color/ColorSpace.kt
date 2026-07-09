package dev.luna5ama.trollhack.graphics.color

sealed interface ColorSpace {
    interface ARGB : ColorSpace
    interface RGBA : ColorSpace
}