package cum.xiaro.trollhack.setting.settings

import kotlin.reflect.KProperty

internal interface NonPrimitive<T : Any> : ISetting<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}

internal interface MutableNonPrimitive<T : Any> : IMutableSetting<T>, NonPrimitive<T> {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}