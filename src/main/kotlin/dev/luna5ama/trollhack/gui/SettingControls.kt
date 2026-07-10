@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.luna5ama.trollhack.gui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.config.settings.AbstractRangedSetting
import dev.luna5ama.trollhack.config.settings.AbstractSetting
import dev.luna5ama.trollhack.config.settings.AbstractSteppingRangedSetting
import dev.luna5ama.trollhack.config.settings.BindSetting
import dev.luna5ama.trollhack.config.settings.BooleanSetting
import dev.luna5ama.trollhack.config.settings.ColorSetting
import dev.luna5ama.trollhack.config.settings.EnumSetting
import dev.luna5ama.trollhack.config.settings.LabelSetting
import dev.luna5ama.trollhack.config.settings.StringListSetting
import dev.luna5ama.trollhack.config.settings.StringSetSetting
import dev.luna5ama.trollhack.config.settings.StringSetting
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.utils.Displayable
import java.util.Locale
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
internal fun SettingsList(module: AbstractModule) {
    val revision = TrollHackCompose.observeRevision()
    val settings = module.filteredSettings.filter { it.isVisible }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        itemsIndexed(settings, key = { _, setting -> System.identityHashCode(setting) }) { index, setting ->
            SettingControl(setting, revision, index)
        }
    }
}

@Composable
private fun SettingControl(setting: AbstractSetting<*, *>, revision: Int, index: Int) {
    when (setting) {
        is LabelSetting -> LabelControl(setting, revision, index)
        is BooleanSetting -> BooleanControl(setting, revision, index)
        is BindSetting -> BindControl(setting, revision, index)
        is EnumSetting<*> -> EnumControl(setting, revision, index)
        is ColorSetting -> ColorControl(setting, revision, index)
        is AbstractRangedSetting<*, *> -> RangedControl(setting, revision, index)
        is StringSetting -> StringControl(setting, revision, index)
        is StringListSetting -> StringListControl(setting, revision, index)
        is StringSetSetting -> StringSetControl(setting, revision, index)
        else -> LegacyRow(index) { SettingName(setting, Modifier.weight(1f)) }
    }
}

@Composable
private fun LegacyRow(
    index: Int,
    background: Color = if (index and 1 == 0) LegacyPalette.Row else LegacyPalette.RowAlt,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth().height(13.dp).background(background).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SettingName(setting: AbstractSetting<*, *>, modifier: Modifier = Modifier) {
    Text(
        setting.nameAsString,
        modifier = modifier,
        color = LegacyPalette.Text,
        fontSize = 8.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun LegacyProgressRow(
    index: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(modifier.fillMaxWidth().height(13.dp).background(rowColor(index))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(LegacyPalette.Enabled))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun BooleanControl(setting: BooleanSetting, revision: Int, index: Int) {
    var value by remember(setting) { mutableStateOf(setting.value) }
    var fillTarget by remember(setting) { mutableStateOf(0f) }
    LaunchedEffect(revision) {
        value = setting.value
        fillTarget = if (value) 1f else 0f
    }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy boolean fill"
    )
    LegacyProgressRow(
        index,
        fillProgress,
        Modifier.legacyPress(setting) {
            setting.value = !setting.value
            value = setting.value
            fillTarget = if (value) 1f else 0f
            TrollHackCompose.refresh()
        }
    ) {
        SettingName(setting, Modifier.weight(1f))
    }
}

@Composable
private fun BindControl(setting: BindSetting, revision: Int, index: Int) {
    var keyName by remember(setting) { mutableStateOf(setting.keyName) }
    LaunchedEffect(revision) { keyName = setting.keyName }
    LegacyRow(index, modifier = Modifier.legacyPress(setting) {
        TrollHackCompose.beginBind(setting)
    }) {
        SettingName(setting, Modifier.weight(1f))
        LegacyValue(if (TrollHackCompose.isBinding(setting)) "..." else keyName)
    }
}

@Composable
private fun EnumControl(setting: EnumSetting<*>, revision: Int, index: Int) {
    var display by remember(setting) { mutableStateOf(enumDisplay(setting.value)) }
    var fillTarget by remember(setting) { mutableStateOf(0f) }
    fun updateMirror() {
        display = enumDisplay(setting.value)
        fillTarget = enumProgress(setting.value)
    }
    LaunchedEffect(revision) { updateMirror() }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy enum fill"
    )
    LegacyProgressRow(
        index,
        fillProgress,
        modifier = Modifier.pointerInput(setting) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Press) continue
                    when (event.button) {
                        PointerButton.Primary -> setting.next()
                        PointerButton.Secondary -> setting.prev()
                        else -> continue
                    }
                    updateMirror()
                    TrollHackCompose.refresh()
                    event.changes.forEach { it.consume() }
                }
            }
        }
    ) {
        SettingName(setting, Modifier.weight(1f))
        LegacyValue(display)
    }
}

@Composable
private fun RangedControl(setting: AbstractRangedSetting<*, *>, revision: Int, index: Int) {
    var visualValue by remember(setting) { mutableStateOf<Any?>(setting.value) }
    var fillTarget by remember(setting) { mutableStateOf(0f) }
    LaunchedEffect(revision) {
        visualValue = setting.value
        fillTarget = settingRatio(setting, visualValue)
    }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy setting fill"
    )
    val modifier = Modifier.pointerInput(setting) {
        fun update(x: Float) {
            setRangedSetting(setting, x / size.width.coerceAtLeast(1))
            visualValue = setting.value
            fillTarget = settingRatio(setting, visualValue)
            TrollHackCompose.refresh()
        }
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                if (event.type == PointerEventType.Press && event.button == PointerButton.Primary ||
                    event.type == PointerEventType.Move && event.buttons.isPrimaryPressed
                ) {
                    update(change.position.x)
                    change.consume()
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(13.dp).background(rowColor(index)).then(modifier)) {
        Box(Modifier.fillMaxWidth(fillProgress).fillMaxHeight().background(LegacyPalette.Enabled))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingName(setting, Modifier.weight(1f))
            LegacyValue(formatSettingValue(visualValue))
        }
    }
}

@Composable
private fun StringControl(setting: StringSetting, revision: Int, index: Int) {
    LegacyStringRow(setting, setting.value, revision, index) {
        setting.value = it
        TrollHackCompose.refresh()
    }
}

@Composable
private fun StringListControl(setting: StringListSetting, revision: Int, index: Int) {
    LegacyStringRow(setting, setting.value.joinToString(", "), revision, index) {
        setting.value = splitCollection(it)
        TrollHackCompose.refresh()
    }
}

@Composable
private fun StringSetControl(setting: StringSetSetting, revision: Int, index: Int) {
    LegacyStringRow(setting, setting.value.joinToString(", "), revision, index) {
        setting.value = splitCollection(it).toSet()
        TrollHackCompose.refresh()
    }
}

@Composable
private fun LegacyStringRow(
    setting: AbstractSetting<*, *>,
    externalValue: String,
    revision: Int,
    index: Int,
    onValueChange: (String) -> Unit
) {
    var focused by remember(setting) { mutableStateOf(false) }
    var fillTarget by remember(setting) { mutableStateOf(0f) }
    var field by remember(setting) {
        mutableStateOf(TextFieldValue(externalValue, TextRange(externalValue.length)))
    }
    LaunchedEffect(revision, focused) {
        if (!focused && field.text != externalValue) {
            field = TextFieldValue(externalValue, TextRange(externalValue.length))
        }
        fillTarget = if (focused) 0f else 1f
    }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy string fill"
    )
    LegacyProgressRow(index, fillProgress) {
        SettingName(setting, Modifier.weight(1f))
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                onValueChange(it.text)
            },
            singleLine = true,
            textStyle = TextStyle(
                color = LegacyPalette.Text,
                fontSize = 8.sp,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(66.dp).onFocusChanged {
                focused = it.isFocused
                fillTarget = if (focused) 0f else 1f
                ClickGuiState.textInputFocused = it.isFocused
            }
        )
    }
}

@Composable
private fun ColorControl(setting: ColorSetting, revision: Int, index: Int) {
    var color by remember(setting) { mutableStateOf(setting.value) }
    var expanded by remember(setting) { mutableStateOf(false) }
    LaunchedEffect(revision) { color = setting.value }
    Column {
        LegacyRow(index, modifier = Modifier.legacyPress(setting) { expanded = !expanded }) {
            SettingName(setting, Modifier.weight(1f))
            Box(Modifier.width(20.dp).height(8.dp).background(Color(color.r, color.g, color.b, color.a)))
        }
        if (expanded) {
            listOf("R" to color.r, "G" to color.g, "B" to color.b, "A" to color.a)
                .forEachIndexed { channel, pair ->
                    ColorChannel(pair.first, pair.second, index + channel + 1) { value ->
                        setting.value = color.withChannel(channel, value)
                        color = setting.value
                        TrollHackCompose.refresh()
                    }
                }
        }
    }
}

@Composable
private fun ColorChannel(name: String, value: Int, index: Int, onChange: (Int) -> Unit) {
    val ratio = value / 255f
    val fillProgress by animateFloatAsState(
        ratio,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy color fill"
    )
    Box(
        Modifier.fillMaxWidth().height(13.dp).background(rowColor(index))
            .pointerInput(name) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Primary ||
                            event.type == PointerEventType.Move && event.buttons.isPrimaryPressed
                        ) {
                            onChange(((change.position.x / size.width) * 255f).roundToInt().coerceIn(0, 255))
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Box(Modifier.fillMaxWidth(fillProgress).fillMaxHeight().background(LegacyPalette.Enabled))
        Row(Modifier.fillMaxSize().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), color = LegacyPalette.Text, fontSize = 8.sp)
            LegacyValue(value.toString())
        }
    }
}

@Composable
private fun LabelControl(setting: LabelSetting, revision: Int, index: Int) {
    var label by remember(setting) { mutableStateOf(setting.label()) }
    LaunchedEffect(revision) { label = setting.label() }
    LegacyRow(index) {
        Text(label, color = LegacyPalette.TextMuted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun LegacyValue(value: String) {
    Text(
        value,
        color = LegacyPalette.Text,
        fontSize = 7.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.End
    )
}

private fun Modifier.legacyPress(key: Any, onPress: () -> Unit) = pointerInput(key) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.button == PointerButton.Primary) {
                onPress()
                event.changes.forEach { it.consume() }
            }
        }
    }
}

private fun rowColor(index: Int) = if (index and 1 == 0) LegacyPalette.Row else LegacyPalette.RowAlt

private fun enumDisplay(value: Any?) =
    (value as? Displayable)?.displayName?.toString() ?: value.toString()

private fun enumProgress(value: Any?): Float {
    val enumValue = value as? Enum<*> ?: return 0f
    val count = enumValue.declaringJavaClass.enumConstants?.size ?: return 0f
    return if (count <= 1) 0f else enumValue.ordinal.toFloat() / (count - 1).toFloat()
}

private fun splitCollection(value: String) = value.split(',', '\n')
    .map(String::trim)
    .filter(String::isNotEmpty)

private fun settingRatio(setting: AbstractRangedSetting<*, *>, value: Any?): Float {
    val start = (setting.range.start as Number).toDouble()
    val end = (setting.range.endInclusive as Number).toDouble()
    if (start == end || value !is Number) return 0f
    return ((value.toDouble() - start) / (end - start)).toFloat().coerceIn(0f, 1f)
}

@Suppress("UNCHECKED_CAST")
private fun setRangedSetting(setting: AbstractRangedSetting<*, *>, ratio: Float) {
    val start = (setting.range.start as Number).toDouble()
    val end = (setting.range.endInclusive as Number).toDouble()
    val raw = start + (end - start) * ratio.coerceIn(0f, 1f)
    val step = (setting as? AbstractSteppingRangedSetting<*, *>)?.step?.let { (it as Number).toDouble() }
        ?.takeIf { it > 0.0 }
    val value = if (step == null) raw else start + round((raw - start) / step) * step
    when (setting.value) {
        is Int -> (setting as AbstractRangedSetting<Int, *>).value = value.roundToInt()
        is Long -> (setting as AbstractRangedSetting<Long, *>).value = value.roundToLong()
        is Float -> (setting as AbstractRangedSetting<Float, *>).value = value.toFloat()
        is Double -> (setting as AbstractRangedSetting<Double, *>).value = value
    }
}

private fun formatSettingValue(value: Any?) = when (value) {
    is Float -> String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')
    is Double -> String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')
    else -> value.toString()
}

private fun ColorRGBA.withChannel(index: Int, value: Int) = when (index) {
    0 -> red(value)
    1 -> green(value)
    2 -> blue(value)
    else -> alpha(value)
}
