package dev.luna5ama.trollhack.setting.settings.impl.other

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.setting.settings.MutableNonPrimitive
import dev.luna5ama.trollhack.setting.settings.MutableSetting
import dev.luna5ama.trollhack.util.asStringOrNull
import java.util.*
import kotlin.reflect.KProperty

class ColorSetting(
    name: CharSequence,
    value: ColorRGB,
    val hasAlpha: Boolean = true,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : MutableSetting<ColorRGB>(
    name,
    value,
    visibility,
    { _, input -> if (!hasAlpha) input.alpha(255) else input },
    description
), MutableNonPrimitive<ColorRGB> {
    override fun setValue(valueIn: String) {
        val anti0x = valueIn.removePrefix("0x")
        val split = anti0x.split(',')

        if (split.size in 3..4) {
            val r = split[0].toIntOrNull() ?: return
            val g = split[1].toIntOrNull() ?: return
            val b = split[2].toIntOrNull() ?: return
            val a = split.getOrNull(3)?.toIntOrNull() ?: 255

            value = ColorRGB(r, g, b, a)
        } else {
            anti0x.toUIntOrNull(16)?.toInt()?.let {
                value = ColorRGB(it)
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ColorRGB {
        return value
    }

    override fun write(): JsonPrimitive {
        return JsonPrimitive(value.rgba.toUInt().toString(16).uppercase(Locale.ROOT))
    }

    override fun read(jsonElement: JsonElement) {
        jsonElement.asStringOrNull?.let { setValue(it.lowercase(Locale.ROOT)) }
    }
}