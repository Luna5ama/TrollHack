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
package dev.luna5ama.trollhack.graphics.color

import java.awt.Color

/**
 * Created by Gebruiker on 18/04/2017.
 */
class ColorHolder {
    var r: Int
    var g: Int
    var b: Int
    var a: Int

    constructor(r: Int, g: Int, b: Int) {
        this.r = r
        this.g = g
        this.b = b
        a = 255
    }

    constructor(r: Int, g: Int, b: Int, a: Int) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun brighter(): ColorHolder {
        return ColorHolder(
            Math.min(r + 10, 255), Math.min(g + 10, 255), Math.min(b + 10, 255),
            a
        )
    }

    fun darker(): ColorHolder {
        return ColorHolder(
            Math.max(r - 10, 0), Math.max(g - 10, 0), Math.max(b - 10, 0),
            a
        )
    }

    fun becomeHex(hex: Int) {
        setR(hex and 0xFF0000 shr 16)
        setG(hex and 0xFF00 shr 8)
        setB(hex and 0xFF)
        setA(255)
    }

    fun setR(r: Int): ColorHolder {
        this.r = r
        return this
    }

    fun setB(b: Int): ColorHolder {
        this.b = b
        return this
    }

    fun setG(g: Int): ColorHolder {
        this.g = g
        return this
    }

    fun setA(a: Int): ColorHolder {
        this.a = a
        return this
    }

    fun clone(): ColorHolder {
        return ColorHolder(r, g, b, a)
    }

    fun toJavaColour(): Color {
        return Color(r, g, b, a)
    }

    companion object {
        fun fromHex(hex: Int): ColorHolder {
            val n = ColorHolder(0, 0, 0)
            n.becomeHex(hex)
            return n
        }

        fun toHex(r: Int, g: Int, b: Int): Int {
            return 0xff shl 24 or (r and 0xff shl 16) or (g and 0xff shl 8) or (b and 0xff)
        }
    }
}