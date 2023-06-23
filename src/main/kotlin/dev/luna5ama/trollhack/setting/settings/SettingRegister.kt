package dev.luna5ama.trollhack.setting.settings

import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.setting.settings.impl.number.DoubleSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.FloatSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting
import dev.luna5ama.trollhack.util.Bind

/**
 * Setting register overloading
 *
 * @param T Type to have extension function for registering setting
 */
interface SettingRegister<T : Any> {

    /** Integer Setting */
    fun T.setting(
        name: CharSequence,
        value: Int,
        range: IntRange,
        step: Int,
        visibility: ((() -> Boolean))? = null,
        consumer: (prev: Int, input: Int) -> Int = { _, input -> input },
        description: CharSequence = "",
        fineStep: Int = step,
        isTransient: Boolean = false
    ) = setting(
        IntegerSetting(
            settingName(name),
            value,
            range,
            step,
            visibility,
            consumer,
            description,
            fineStep,
            isTransient
        )
    )

    /** Double Setting */
    fun T.setting(
        name: CharSequence,
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
        visibility: ((() -> Boolean))? = null,
        consumer: (prev: Double, input: Double) -> Double = { _, input -> input },
        description: CharSequence = "",
        fineStep: Double = step,
        isTransient: Boolean = false
    ) = setting(
        DoubleSetting(
            settingName(name),
            value,
            range,
            step,
            visibility,
            consumer,
            description,
            fineStep,
            isTransient
        )
    )

    /** Float Setting */
    fun T.setting(
        name: CharSequence,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        step: Float,
        visibility: ((() -> Boolean))? = null,
        consumer: (prev: Float, input: Float) -> Float = { _, input -> input },
        description: CharSequence = "",
        fineStep: Float = step,
        isTransient: Boolean = false
    ) = setting(
        FloatSetting(
            settingName(name),
            value,
            range,
            step,
            visibility,
            consumer,
            description,
            fineStep,
            isTransient
        )
    )

    /** Bind Setting */
    fun T.setting(
        name: CharSequence,
        value: Bind,
        action: ((Boolean) -> Unit)? = null,
        visibility: ((() -> Boolean))? = null,
        description: CharSequence = "",
        isTransient: Boolean = false
    ) = setting(BindSetting(settingName(name), value, visibility, action, description, isTransient))

    /** Color Setting */
    fun T.setting(
        name: CharSequence,
        value: ColorRGB,
        hasAlpha: Boolean = true,
        visibility: ((() -> Boolean))? = null,
        description: CharSequence = "",
        isTransient: Boolean = false
    ) = setting(ColorSetting(settingName(name), value, hasAlpha, visibility, description, isTransient))

    /** Boolean Setting */
    fun T.setting(
        name: CharSequence,
        value: Boolean,
        visibility: ((() -> Boolean))? = null,
        consumer: (prev: Boolean, input: Boolean) -> Boolean = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ) = setting(BooleanSetting(settingName(name), value, visibility, consumer, description, isTransient))

    /** Enum Setting */
    fun <E : Enum<E>> T.setting(
        name: CharSequence,
        value: E,
        visibility: (() -> Boolean)? = null,
        consumer: (prev: E, input: E) -> E = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ) = setting(EnumSetting(settingName(name), value, visibility, consumer, description, isTransient))

    /** String Setting */
    fun T.setting(
        name: CharSequence,
        value: String,
        visibility: (() -> Boolean)? = null,
        consumer: (prev: String, input: String) -> String = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ) = setting(StringSetting(settingName(name), value, visibility, consumer, description, isTransient))
    /* End of setting registering */

    /**
     * Register a setting
     *
     * @param S Type of the setting
     * @param setting Setting to register
     *
     * @return [setting]
     */
    fun <S : AbstractSetting<*>> T.setting(setting: S): S

    fun settingName(input: CharSequence): CharSequence {
        return input
    }
}