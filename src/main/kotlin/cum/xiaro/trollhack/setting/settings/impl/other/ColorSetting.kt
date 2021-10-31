package cum.xiaro.trollhack.setting.settings.impl.other

import cum.xiaro.trollhack.util.graphics.ColorRGB
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableNonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableSetting
import cum.xiaro.trollhack.util.asStringOrNull
import kotlin.reflect.KProperty

class ColorSetting(
    name: CharSequence,
    value: ColorRGB,
    val hasAlpha: Boolean = true,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = ""
) : MutableSetting<ColorRGB>(name, value, visibility, { _, input -> if (!hasAlpha) input.alpha(255) else input }, description), MutableNonPrimitive<ColorRGB> {
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
            anti0x.toIntOrNull(16)?.let {
                value = ColorRGB(it)
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ColorRGB {
        return value
    }

    override fun write(): JsonPrimitive {
        return JsonPrimitive(value.rgba.toString(16))
    }

    override fun read(jsonElement: JsonElement) {
        jsonElement.asStringOrNull?.let { setValue(it) }
    }
}