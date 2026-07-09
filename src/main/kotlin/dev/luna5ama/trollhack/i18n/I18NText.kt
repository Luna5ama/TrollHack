package dev.luna5ama.trollhack.i18n

import dev.luna5ama.trollhack.utils.state.FrameValue
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class I18NText(
    val translateKey: String, private val i18N: I18N, var defaultText: String = translateKey
) : ReadOnlyProperty<Any?, String>, PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, String>> {
    private val localizedTexts = mutableMapOf<Lang, String>()

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return this[i18N.currentLang]
    }

    override fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
        return object : ReadOnlyProperty<Any?, String> {
            private val lazyText by FrameValue { this@I18NText[i18N.currentLang] }

            override fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return lazyText
            }
        }
    }

    operator fun set(lang: Lang, value: String) {
        localizedTexts[lang] = value
    }

    operator fun get(lang: Lang): String {
        return localizedTexts[lang] ?: localizedTexts[Lang.ENGLISH] ?: defaultText
    }
}