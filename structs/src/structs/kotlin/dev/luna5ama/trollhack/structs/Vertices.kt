package dev.luna5ama.trollhack.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
interface Pos2Color {
    val pos : Vec2f32
    val color: Int
}

@Struct
interface Pos3Color {
    val pos : Vec3f32
    val color: Int
}

@Struct
interface FontVertex {
    val position: Vec2f32
    val vertUV: Vec2i16
    val colorIndex: Byte
    val overrideColor: Byte
    val shadow: Byte
}