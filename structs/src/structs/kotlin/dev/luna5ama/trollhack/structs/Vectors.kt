package dev.luna5ama.trollhack.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
interface Vec4i8 {
    val x: Byte
    val y: Byte
    val z: Byte
    val w: Byte
}

@Struct
interface Vec2i16 {
    val x: Short
    val y: Short
}

@Struct
interface Vec2f32 {
    val x: Float
    val y: Float
}

@Struct
interface Vec3f32 {
    val x: Float
    val y: Float
    val z: Float
}

@Struct
interface Vec4f32 {
    val x: Float
    val y: Float
    val z: Float
    val w: Float
}