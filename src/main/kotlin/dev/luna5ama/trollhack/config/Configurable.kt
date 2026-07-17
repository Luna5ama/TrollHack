package dev.luna5ama.trollhack.config

import dev.luna5ama.trollhack.config.filesystem.Path
import dev.luna5ama.trollhack.config.settings.*
import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.i18n.ILocalizedNameable
import dev.luna5ama.trollhack.i18n.Lang
import dev.luna5ama.trollhack.manager.managers.ConfigManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.*
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.input.KeyBind
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty

private val settingSeparator = Regex("[_-]+")
private val settingAcronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
private val settingWordBoundary = Regex("([a-z])([A-Z])")
private val settingNumberBoundary = Regex("([a-z])(\\d+)")
private val settingNumberWordBoundary = Regex("(\\d)([A-Z][a-z])")
private val settingDimensionBoundary = Regex("(\\d)x (\\d)")

internal fun normalizeSettingName(name: String): String {
    if (name.startsWith("__internal__")) return name

    return name
        .replace(settingSeparator, " ")
        .replace(settingAcronymBoundary, "$1 $2")
        .replace(settingWordBoundary, "$1 $2")
        .replace(settingNumberBoundary, "$1 $2")
        .replace(settingNumberWordBoundary, "$1 $2")
        .replace(settingDimensionBoundary, "$1x$2")
        .trim()
        .split(Regex(" +"))
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

private fun ILocalizedNameable.resolveSetting(name: String) =
    "$translateKey.${normalizeSettingName(name).lowercase()}"

interface Configurable {
    val excluded: Boolean get() = false
    val configCategory: String
    val settings: MutableList<AbstractSetting<*, *>>

    fun label(
        translateKey: String,
        i18N: I18N,
        description: String = "",
        visibility: Predicate<Unit> = always(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
        title: () -> String = { normalizeSettingName(defaultName) }
    ) = setting(LabelSetting(translateKey, i18N, description, visibility, normalizeSettingName(defaultName), title)).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.label(
        name: String,
        title: () -> String,
        concatWithName: Boolean = false,
        visibility: Predicate<Unit> = always(),
        description: String = "",
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap()
    ) = label(resolveSetting(name), i18N, description, visibility, defaultName, defaultTranslations) {
        val localizedName = i18N[resolveSetting(name), normalizeSettingName(defaultName)][ClientSettings.modLanguage]
        if (concatWithName) localizedName + ": " + title() else title()
    }

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: KeyBind,
        visibility: Predicate<KeyBind> = always(),
        description: String = "",
        onModified: List<BiPredicate<KeyBind, KeyBind>> = listOf(),
        transformer: Combiner<KeyBind> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
        alwaysActive: Boolean = false
    ) = setting(BindSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName), alwaysActive)).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: KeyBind,
        visibility: Predicate<KeyBind> = always(),
        description: String = "",
        onModified: List<BiPredicate<KeyBind, KeyBind>> = listOf(),
        transformer: Combiner<KeyBind> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
        alwaysActive: Boolean = false
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations, alwaysActive)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: String,
        visibility: Predicate<String> = always(),
        description: String = "",
        onModified: List<BiPredicate<String, String>> = listOf(),
        transformer: Combiner<String> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(StringSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: String,
        visibility: Predicate<String> = always(),
        description: String = "",
        onModified: List<BiPredicate<String, String>> = listOf(),
        transformer: Combiner<String> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Boolean,
        visibility: Predicate<Boolean> = always(),
        description: String = "",
        onModified: List<BiPredicate<Boolean, Boolean>> = listOf(),
        transformer: Combiner<Boolean> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(BooleanSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Boolean,
        visibility: Predicate<Boolean> = always(),
        description: String = "",
        onModified: List<BiPredicate<Boolean, Boolean>> = listOf(),
        transformer: Combiner<Boolean> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun <E> setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: E,
        visibility: Predicate<E> = always(),
        description: String = "",
        onModified: List<BiPredicate<E, E>> = listOf(),
        transformer: Combiner<E> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) : EnumSetting<E> where E : Enum<E>, E : Displayable =
        setting(EnumSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
            defaultTranslations.forEach { (lang, text) ->
                i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
            }
        }

    fun <E> ILocalizedNameable.setting(
        name: String,
        defaultValue: E,
        visibility: Predicate<E> = always(),
        description: String = "",
        onModified: List<BiPredicate<E, E>> = listOf(),
        transformer: Combiner<E> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) : EnumSetting<E> where E : Enum<E>, E : Displayable =
        setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)


    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: List<String>,
        visibility: Predicate<List<String>> = always(),
        description: String = "",
        onModified: List<BiPredicate<List<String>, List<String>>> = listOf(),
        transformer: Combiner<List<String>> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(StringListSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: List<String>,
        visibility: Predicate<List<String>> = always(),
        description: String = "",
        onModified: List<BiPredicate<List<String>, List<String>>> = listOf(),
        transformer: Combiner<List<String>> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)


    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Set<String>,
        visibility: Predicate<Set<String>> = always(),
        description: String = "",
        onModified: List<BiPredicate<Set<String>, Set<String>>> = listOf(),
        transformer: Combiner<Set<String>> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(StringSetSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Set<String>,
        visibility: Predicate<Set<String>> = always(),
        description: String = "",
        onModified: List<BiPredicate<Set<String>, Set<String>>> = listOf(),
        transformer: Combiner<Set<String>> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)


    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: ColorRGBA,
        visibility: Predicate<ColorRGBA> = always(),
        description: String = "",
        onModified: List<BiPredicate<ColorRGBA, ColorRGBA>> = listOf(),
        transformer: Combiner<ColorRGBA> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(ColorSetting(translateKey, i18N, defaultValue, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: ColorRGBA,
        visibility: Predicate<ColorRGBA> = always(),
        description: String = "",
        onModified: List<BiPredicate<ColorRGBA, ColorRGBA>> = listOf(),
        transformer: Combiner<ColorRGBA> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Int,
        range: ClosedRange<Int>,
        step: Int = 1,
        visibility: Predicate<Int> = always(),
        description: String = "",
        onModified: List<BiPredicate<Int, Int>> = listOf(),
        transformer: Combiner<Int> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(IntSetting(translateKey, i18N, defaultValue, range, step, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Int,
        range: ClosedRange<Int>,
        step: Int = 1,
        visibility: Predicate<Int> = always(),
        description: String = "",
        onModified: List<BiPredicate<Int, Int>> = listOf(),
        transformer: Combiner<Int> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, range, step, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Long,
        range: ClosedRange<Long>,
        step: Long = 1,
        visibility: Predicate<Long> = always(),
        description: String = "",
        onModified: List<BiPredicate<Long, Long>> = listOf(),
        transformer: Combiner<Long> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(LongSetting(translateKey, i18N, defaultValue, range, step, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Long,
        range: ClosedRange<Long>,
        step: Long = 1,
        visibility: Predicate<Long> = always(),
        description: String = "",
        onModified: List<BiPredicate<Long, Long>> = listOf(),
        transformer: Combiner<Long> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, range, step, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Float,
        range: ClosedRange<Float>,
        step: Float = 0.1f,
        visibility: Predicate<Float> = always(),
        description: String = "",
        onModified: List<BiPredicate<Float, Float>> = listOf(),
        transformer: Combiner<Float> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(FloatSetting(translateKey, i18N, defaultValue, range, step, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Float,
        range: ClosedRange<Float>,
        step: Float = 0.1f,
        visibility: Predicate<Float> = always(),
        description: String = "",
        onModified: List<BiPredicate<Float, Float>> = listOf(),
        transformer: Combiner<Float> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, range, step, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)

    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Double,
        range: ClosedRange<Double>,
        step: Double = 0.1,
        visibility: Predicate<Double> = always(),
        description: String = "",
        onModified: List<BiPredicate<Double, Double>> = listOf(),
        transformer: Combiner<Double> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(DoubleSetting(translateKey, i18N, defaultValue, range, step, description, visibility, onModified.toMutableList(), transformer, normalizeSettingName(defaultName))).apply {
        defaultTranslations.forEach { (lang, text) ->
            i18N[translateKey][lang] = if (lang == Lang.ENGLISH) normalizeSettingName(text) else text
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Double,
        range: ClosedRange<Double>,
        step: Double = 0.1,
        visibility: Predicate<Double> = always(),
        description: String = "",
        onModified: List<BiPredicate<Double, Double>> = listOf(),
        transformer: Combiner<Double> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, range, step, visibility, description, onModified.toMutableList(), transformer, defaultName, defaultTranslations)


    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Int,
        visibility: Predicate<Int> = always(),
        description: String = "",
        onModified: List<BiPredicate<Int, Int>> = listOf(),
        transformer: Combiner<Int> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ): WrappedSetting<Int> {
        val onModified0 = ArrayList(onModified.map { BiPredicate { prev: String, value: String -> it(prev.toIntOrNull() ?: defaultValue, value.toIntOrNull() ?: defaultValue) } })
        val visibility0 = Predicate { value: String -> visibility(value.toIntOrNull() ?: defaultValue) }
        val transformer0 = Combiner { value1: String, value2: String -> transformer(value1.toIntOrNull() ?: defaultValue, value2.toIntOrNull() ?: defaultValue).toString() }
        val delegated = this@Configurable.setting(translateKey, i18N, defaultValue.toString(), visibility0, description, onModified0, transformer0, defaultName, defaultTranslations)
        return object : WrappedSetting<Int> {
            override val delegate: AbstractSetting<String, *>
                get() = delegated

            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                return delegated.value.toIntOrNull() ?: defaultValue
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                val str = value.toString()
                delegated.value = str
            }
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Int,
        visibility: Predicate<Int> = always(),
        description: String = "",
        onModified: List<BiPredicate<Int, Int>> = listOf(),
        transformer: Combiner<Int> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified, transformer, defaultName, defaultTranslations)


    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Float,
        visibility: Predicate<Float> = always(),
        description: String = "",
        onModified: List<BiPredicate<Float, Float>> = listOf(),
        transformer: Combiner<Float> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ): WrappedSetting<Float> {
        val onModified0 = ArrayList(onModified.map { BiPredicate { prev: String, value: String -> it(prev.toFloatOrNull() ?: defaultValue, value.toFloatOrNull() ?: defaultValue) } })
        val visibility0 = Predicate { value: String -> visibility(value.toFloatOrNull() ?: defaultValue) }
        val transformer0 = Combiner { value1: String, value2: String -> transformer(value1.toFloatOrNull() ?: defaultValue, value2.toFloatOrNull() ?: defaultValue).toString() }
        val delegated = this@Configurable.setting(translateKey, i18N, defaultValue.toString(), visibility0, description, onModified0, transformer0, defaultName, defaultTranslations)
        return object : WrappedSetting<Float> {
            override val delegate: AbstractSetting<String, *>
                get() = delegated

            override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
                return delegated.value.toFloatOrNull() ?: defaultValue
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
                val str = value.toString()
                delegated.value = str
            }
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Float,
        visibility: Predicate<Float> = always(),
        description: String = "",
        onModified: List<BiPredicate<Float, Float>> = listOf(),
        transformer: Combiner<Float> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified, transformer, defaultName, defaultTranslations)
    
    
    fun setting(
        translateKey: String,
        i18N: I18N,
        defaultValue: Double,
        visibility: Predicate<Double> = always(),
        description: String = "",
        onModified: List<BiPredicate<Double, Double>> = listOf(),
        transformer: Combiner<Double> = reflBi(),
        defaultName: String = translateKey,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ): WrappedSetting<Double> {
        val onModified0 = ArrayList(onModified.map { BiPredicate { prev: String, value: String -> it(prev.toDoubleOrNull() ?: defaultValue, value.toDoubleOrNull() ?: defaultValue) } })
        val visibility0 = Predicate { value: String -> visibility(value.toDoubleOrNull() ?: defaultValue) }
        val transformer0 = Combiner { value1: String, value2: String -> transformer(value1.toDoubleOrNull() ?: defaultValue, value2.toDoubleOrNull() ?: defaultValue).toString() }
        val delegated = this@Configurable.setting(translateKey, i18N, defaultValue.toString(), visibility0, description, onModified0, transformer0, defaultName, defaultTranslations)
        return object : WrappedSetting<Double> {
            override val delegate: AbstractSetting<String, *>
                get() = delegated

            override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
                return delegated.value.toDoubleOrNull() ?: defaultValue
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
                val str = value.toString()
                delegated.value = str
            }
        }
    }

    fun ILocalizedNameable.setting(
        name: String,
        defaultValue: Double,
        visibility: Predicate<Double> = always(),
        description: String = "",
        onModified: List<BiPredicate<Double, Double>> = listOf(),
        transformer: Combiner<Double> = reflBi(),
        defaultName: String = name,
        defaultTranslations: Map<Lang, String> = emptyMap(),
    ) = setting(resolveSetting(name), i18N, defaultValue, visibility, description, onModified, transformer, defaultName, defaultTranslations)
    
    fun <S : AbstractSetting<*, *>> setting(setting: S): S

    open class NamedConfigurable(
        override val name: CharSequence,
        override val configCategory: String,
        override val excluded: Boolean = false
    ) : Nameable, Configurable {
        constructor(
            base: CharSequence,
            vararg subpaths: CharSequence,
            configCategory: String,
            excluded: Boolean
        ) : this(Path(base.toString(), *subpaths.map { it.toString() }.toTypedArray()).toString(), configCategory, excluded)

        override val settings: MutableList<AbstractSetting<*, *>> = mutableListOf()

        override fun <S : AbstractSetting<*, *>> setting(setting: S): S {
            settings.add(setting)
            return setting
        }

        override fun toString(): String {
            return "NamedConfigurable(name=$name, configCategory='$configCategory')"
        }

        init {
            ConfigManager.getOrCreateCategory(configCategory).addConfigurable(this)
        }
    }

    open class AnonymousConfigurable(override val configCategory: String) : Nameable, Configurable {
        override val name: CharSequence = "AnonymousConfigurable${id.getAndIncrement()}"
        override val settings: MutableList<AbstractSetting<*, *>> = mutableListOf()

        init {
            ConfigManager.getOrCreateCategory(configCategory).addConfigurable(this)
        }

        override fun <S : AbstractSetting<*, *>> setting(setting: S): S {
            settings.add(setting)
            return setting
        }

        override fun toString(): String {
            return "AnonymousConfigurable(configCategory='$configCategory', name=$name)"
        }

        companion object {
            private val id = AtomicInteger(0)
        }
    }
}
