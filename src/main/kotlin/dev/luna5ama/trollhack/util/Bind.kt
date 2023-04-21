package dev.luna5ama.trollhack.util

import it.unimi.dsi.fastutil.ints.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class Bind(
    modifierKeysIn: IntAVLTreeSet,
    keyIn: Int
) {

    constructor() : this(0)

    constructor(key: Int) : this(IntAVLTreeSet(keyComparator), key)

    constructor(
        vararg modifierKeys: Int,
        key: Int
    ) : this(IntAVLTreeSet(keyComparator).apply { modifierKeys.forEach { add(it) } }, key)

    val modifierKeys = modifierKeysIn
    var key = keyIn; private set
    private var cachedName = getName()

    val isEmpty get() = key !in 1..255 && key !in -16..-1

    fun isDown(): Boolean {
        return !isEmpty
            && if (key >= 0) Keyboard.isKeyDown(key) else Mouse.isButtonDown(-key - 1)
            && synchronized(this) { modifierKeys.all { isModifierKeyDown(key, it) } }
    }

    fun isDown(eventKey: Int): Boolean {
        return !isEmpty
            && key == eventKey
            && synchronized(this) { modifierKeys.all { isModifierKeyDown(eventKey, it) } }
    }

    private fun isModifierKeyDown(eventKey: Int, modifierKey: Int) =
        eventKey != modifierKey
            && when (modifierKey) {
            Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> {
                Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
            }
            Keyboard.KEY_LMENU, Keyboard.KEY_RMENU -> {
                Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
            }
            Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT -> {
                Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
            }
            Keyboard.KEY_LMETA, Keyboard.KEY_RMETA -> {
                Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)
            }
            in 0..255 -> {
                Keyboard.isKeyDown(modifierKey)
            }
            else -> {
                false
            }
        }

    fun setBind(keyIn: Int) {
        val cache = IntArrayList(0)

        for (key in Keyboard.KEYBOARD_SIZE - 1 downTo 1) {
            if (key == keyIn) continue
            if (!Keyboard.isKeyDown(key)) continue
            cache.add(key)
        }

        setBind(cache, keyIn)
    }

    fun setBind(modifierKeysIn: IntCollection, keyIn: Int) {
        synchronized(this) {
            modifierKeys.clear()
            modifierKeys.addAll(modifierKeysIn)
            key = keyIn
            cachedName = getName()
        }
    }

    fun clear() {
        synchronized(this) {
            modifierKeys.clear()
            key = 0
            cachedName = getName()
        }
    }

    override fun toString(): String {
        return cachedName
    }

    private fun getName(): String {
        return if (isEmpty) {
            "None"
        } else {
            StringBuilder().run {
                for (key in modifierKeys) {
                    val name = modifierName[key] ?: KeyboardUtils.getDisplayName(key) ?: continue
                    append(name)
                    append('+')
                }
                if (key >= 0) {
                    append(KeyboardUtils.getDisplayName(key))
                } else {
                    append("Mouse")
                    append(' ')
                    append(-key - 1)
                }
                toString()
            }
        }
    }

    companion object {
        private val modifierName = Int2ObjectLinkedOpenHashMap<String>().apply {
            put(Keyboard.KEY_LCONTROL, "Ctrl")
            put(Keyboard.KEY_RCONTROL, "Ctrl")
            put(Keyboard.KEY_LMENU, "Alt")
            put(Keyboard.KEY_RMENU, "Alt")
            put(Keyboard.KEY_LSHIFT, "Shift")
            put(Keyboard.KEY_RSHIFT, "Shift")
            put(Keyboard.KEY_LMETA, "Meta")
            put(Keyboard.KEY_RMETA, "Meta")
        }

        private val priorityMap = Int2IntLinkedOpenHashMap().apply {
            val priorityKey = arrayOf(
                Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL,
                Keyboard.KEY_LMENU, Keyboard.KEY_RMENU,
                Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT,
                Keyboard.KEY_LMETA, Keyboard.KEY_RMETA
            )

            for ((index, key) in priorityKey.withIndex()) {
                this[key] = index / 2
            }

            val sortedKeys = KeyboardUtils.allKeys.sortedBy { Keyboard.getKeyName(it) }

            for ((index, key) in sortedKeys.withIndex()) {
                this.putIfAbsent(key, index + priorityKey.size / 2)
            }
        }

        val keyComparator = compareBy<Int> {
            priorityMap[it]
        }
    }
}