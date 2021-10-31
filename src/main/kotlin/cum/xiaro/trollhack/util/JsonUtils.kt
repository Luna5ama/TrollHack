package cum.xiaro.trollhack.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

val JsonElement.asJsonArrayOrNull: JsonArray? get() = (this as? JsonArray)
val JsonElement.asJsonPrimitiveOrNull: JsonPrimitive? get() = (this as? JsonPrimitive)

val JsonElement.asBooleanOrNull: Boolean? get() = runCatching { this.asBoolean }.getOrNull()
val JsonElement.asIntOrNull: Int? get() = runCatching { this.asInt }.getOrNull()
val JsonElement.asFloatOrNull: Float? get() = runCatching { this.asFloat }.getOrNull()
val JsonElement.asDoubleOrNull: Double? get() = runCatching { this.asDouble }.getOrNull()
val JsonElement.asStringOrNull: String? get() = runCatching { this.asString }.getOrNull()