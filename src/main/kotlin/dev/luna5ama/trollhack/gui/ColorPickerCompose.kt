@file:OptIn(ExperimentalComposeUiApi::class)

package dev.luna5ama.trollhack.gui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.config.settings.ColorSetting
import dev.luna5ama.trollhack.graphics.color.ColorHSVA
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.i18n.Lang
import dev.luna5ama.trollhack.utils.always
import dev.luna5ama.trollhack.utils.reflBi
import kotlin.collections.listOf
import kotlin.math.roundToInt

private val PickerWidth = 252.dp
private val PickerHeight = 116.dp
private val PickerTitleHeight = 16.dp
private val PickerFieldSize = 96.dp
private val PickerSliderWidth = 128.dp
private val PickerComponentHeight = 12.dp
private val PickerButtonWidth = 50.dp

@Preview(
    name = "Color Picker Preview",
    widthDp = 300,
    heightDp = 160,
    showBackground = true,
    backgroundColor = 0xFF101214
)
@Composable
internal fun ColorPickerWindowPreview() {
    TrollHackTheme {
        val setting = remember {
            ColorSetting(
                "123",
                I18N(mapOf()) { Lang.ENGLISH },
                ColorRGBA(0xFFFFFFFF.toInt()),
                "",
                always(),
                mutableListOf(),
                reflBi()
            )
        }
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                }
            ) {
                ColorPickerWindow(
                    setting = setting,
                    viewportWidth = 300,
                    viewportHeight = 160,
                    initiallyExpanded = true
                )
            }
        }
    }
}

@Composable
internal fun ColorPickerWindow(
    setting: ColorSetting,
    viewportWidth: Int,
    viewportHeight: Int,
    initiallyExpanded: Boolean = false
) {
    TrollHackCompose.observeRevision()

    val initialColor = remember(setting) { setting.value }
    val initialHsb = remember(setting) { initialColor.toHSB() }
    var draft by remember(setting) { mutableStateOf(initialColor) }
    var hue by remember(setting) { mutableStateOf(initialHsb.h) }
    var saturation by remember(setting) { mutableStateOf(initialHsb.s) }
    var brightness by remember(setting) { mutableStateOf(initialHsb.b) }

    fun syncDraftFromRgb(color: ColorRGBA) {
        draft = color
        val hsb = color.toHSB()
        hue = hsb.h
        saturation = hsb.s
        brightness = hsb.b
    }

    fun syncDraftFromHsb() {
        val rgb = ColorHSVA(hue, saturation, brightness).toRGBA()
        draft = ColorRGBA(rgb.r, rgb.g, rgb.b, draft.a)
    }

    fun setChannel(channel: Int, value: Int) {
        val updated = when (channel) {
            0 -> ColorRGBA(value, draft.g, draft.b, draft.a)
            1 -> ColorRGBA(draft.r, value, draft.b, draft.a)
            2 -> ColorRGBA(draft.r, draft.g, value, draft.a)
            else -> ColorRGBA(draft.r, draft.g, draft.b, value)
        }
        if (channel == 3) {
            draft = updated
        } else {
            syncDraftFromRgb(updated)
        }
    }

    fun applyDraft() {
        setting.value = draft
        syncDraftFromRgb(setting.value)
        TrollHackCompose.refresh()
    }

    var expanded by remember(setting, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    LaunchedEffect(setting) { expanded = true }
    val animatedHeight by animateDpAsState(
        if (expanded) PickerHeight else PickerTitleHeight,
        tween(LegacyWindowDuration, easing = LegacyOutQuart),
        label = "Legacy color picker height"
    )

    val density = LocalDensity.current.density
    val widthPx = PickerWidth.value * density
    val heightPx = PickerHeight.value * density
    val centeredPosition = Offset(
        (viewportWidth - widthPx) * 0.5f,
        (viewportHeight - heightPx) * 0.5f
    )
    var draggedPosition by remember(setting) { mutableStateOf<Offset?>(null) }

    fun clampPosition(position: Offset) = Offset(
        position.x.coerceIn(2f, (viewportWidth - widthPx - 2f).coerceAtLeast(2f)),
        position.y.coerceIn(2f, (viewportHeight - heightPx - 2f).coerceAtLeast(2f))
    )

    val pickerPosition = clampPosition(draggedPosition ?: centeredPosition)
    val appliedColor = setting.value

    Box(
        Modifier.offset { IntOffset(pickerPosition.x.roundToInt(), pickerPosition.y.roundToInt()) }
            .width(PickerWidth)
            .height(animatedHeight)
            .clipToBounds()
            .background(LegacyPalette.WindowStrong)
            .border(0.5.dp, LegacyPalette.Accent)
            .onGloballyPositioned { ClickGuiState.colorPickerBounds = it.boundsInRoot() }
    ) {
        ColorPickerCanvas(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            previous = appliedColor,
            current = draft
        )

        Box(
            Modifier.fillMaxWidth().height(PickerTitleHeight)
                .pointerInput(setting, viewportWidth, viewportHeight) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        draggedPosition = clampPosition((draggedPosition ?: centeredPosition) + amount)
                    }
                }
                .padding(start = 3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "Color Picker",
                color = LegacyPalette.Text,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                style = LegacyTextStyle
            )
        }

        Box(
            Modifier.offset(4.dp, PickerTitleHeight).size(PickerFieldSize)
                .pointerInput(setting) {
                    fun update(position: Offset) {
                        saturation = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        brightness = (1f - position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        syncDraftFromHsb()
                    }
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (
                                event.type == PointerEventType.Press && event.button == PointerButton.Primary ||
                                event.type == PointerEventType.Move && event.buttons.isPrimaryPressed
                            ) {
                                update(change.position)
                                change.consume()
                            }
                        }
                    }
                }
        )

        Box(
            Modifier.offset(106.dp, PickerTitleHeight).width(8.dp).height(PickerFieldSize)
                .pointerInput(setting) {
                    fun update(y: Float) {
                        hue = (y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        syncDraftFromHsb()
                    }
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (
                                event.type == PointerEventType.Press && event.button == PointerButton.Primary ||
                                event.type == PointerEventType.Move && event.buttons.isPrimaryPressed
                            ) {
                                update(change.position.y)
                                change.consume()
                            }
                        }
                    }
                }
        )

        ColorPickerSlider(
            "Red",
            draft.r,
            Modifier.offset(120.dp, 16.dp)
        ) { setChannel(0, it) }
        ColorPickerSlider(
            "Green",
            draft.g,
            Modifier.offset(120.dp, 30.dp)
        ) { setChannel(1, it) }
        ColorPickerSlider(
            "Blue",
            draft.b,
            Modifier.offset(120.dp, 44.dp)
        ) { setChannel(2, it) }
        ColorPickerSlider(
            "Alpha",
            draft.a,
            Modifier.offset(120.dp, 58.dp)
        ) { setChannel(3, it) }

        ColorPickerButton(
            "Okay",
            Modifier.offset(198.dp, 72.dp)
        ) {
            applyDraft()
            ClickGuiState.closeColorPicker()
        }
        ColorPickerButton(
            "Cancel",
            Modifier.offset(198.dp, 86.dp)
        ) {
            ClickGuiState.closeColorPicker()
        }
        ColorPickerButton(
            "Apply",
            Modifier.offset(198.dp, 100.dp)
        ) {
            applyDraft()
        }
    }
}

@Composable
private fun ColorPickerCanvas(
    hue: Float,
    saturation: Float,
    brightness: Float,
    previous: ColorRGBA,
    current: ColorRGBA
) {
    Canvas(Modifier.size(PickerWidth, PickerHeight)) {
        val fieldLeft = 4.dp.toPx()
        val fieldTop = PickerTitleHeight.toPx()
        val fieldSize = PickerFieldSize.toPx()
        val fieldRight = fieldLeft + fieldSize
        val fieldBottom = fieldTop + fieldSize
        val hueLeft = 106.dp.toPx()
        val hueWidth = 8.dp.toPx()
        val hueRight = hueLeft + hueWidth

        val pureHue = ColorHSVA(hue, 1f, 1f).toRGBA().toComposeColor()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, pureHue),
                startX = fieldLeft,
                endX = fieldRight
            ),
            topLeft = Offset(fieldLeft, fieldTop),
            size = Size(fieldSize, fieldSize)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = fieldTop,
                endY = fieldBottom
            ),
            topLeft = Offset(fieldLeft, fieldTop),
            size = Size(fieldSize, fieldSize)
        )

        val markerGray = ((1f - (1f - saturation) * brightness) * 255f).roundToInt().coerceIn(0, 255)
        drawCircle(
            color = Color(markerGray, markerGray, markerGray),
            radius = 4.dp.toPx(),
            center = Offset(
                fieldLeft + fieldSize * saturation,
                fieldTop + fieldSize * (1f - brightness)
            ),
            style = Stroke(1.5.dp.toPx())
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                ),
                startY = fieldTop,
                endY = fieldBottom
            ),
            topLeft = Offset(hueLeft, fieldTop),
            size = Size(hueWidth, fieldSize)
        )

        val pointerY = fieldTop + fieldSize * hue
        val pointerHalfHeight = 2.dp.toPx()
        val pointerStroke = Stroke(1.5.dp.toPx())
        val leftPointer = Path().apply {
            moveTo(hueLeft - 5.dp.toPx(), pointerY - pointerHalfHeight)
            lineTo(hueLeft - 5.dp.toPx(), pointerY + pointerHalfHeight)
            lineTo(hueLeft - 1.dp.toPx(), pointerY)
            close()
        }
        val rightPointer = Path().apply {
            moveTo(hueRight + 1.dp.toPx(), pointerY)
            lineTo(hueRight + 5.dp.toPx(), pointerY + pointerHalfHeight)
            lineTo(hueRight + 5.dp.toPx(), pointerY - pointerHalfHeight)
            close()
        }
        drawPath(leftPointer, LegacyPalette.Accent, style = pointerStroke)
        drawPath(rightPointer, LegacyPalette.Accent, style = pointerStroke)

        drawRect(
            previous.toComposeColor(opaque = true),
            topLeft = Offset(120.dp.toPx(), 72.dp.toPx()),
            size = Size(35.dp.toPx(), 40.dp.toPx())
        )
        drawRect(
            current.toComposeColor(opaque = true),
            topLeft = Offset(159.dp.toPx(), 72.dp.toPx()),
            size = Size(35.dp.toPx(), 40.dp.toPx())
        )
    }
}

@Composable
private fun ColorPickerSlider(
    name: String,
    value: Int,
    modifier: Modifier,
    onChange: (Int) -> Unit
) {
    var fillTarget by remember(name) { mutableStateOf(0f) }
    LaunchedEffect(value) { fillTarget = value / 255f }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy color picker $name fill"
    )
    val currentOnChange by rememberUpdatedState(onChange)

    Box(
        modifier.width(PickerSliderWidth).height(PickerComponentHeight)
            .pointerInput(name) {
                fun update(x: Float) {
                    currentOnChange(((x / size.width.coerceAtLeast(1)) * 255f).roundToInt().coerceIn(0, 255))
                }
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (
                            event.type == PointerEventType.Press && event.button == PointerButton.Primary ||
                            event.type == PointerEventType.Move && event.buttons.isPrimaryPressed
                        ) {
                            update(change.position.x)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Box(Modifier.fillMaxWidth(fillProgress.coerceIn(0f, 1f)).fillMaxHeight().background(LegacyPalette.Enabled))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                color = LegacyPalette.Text,
                fontSize = 8.sp,
                style = LegacyTextStyle
            )
            Text(
                value.toString(),
                color = LegacyPalette.Text,
                fontSize = 7.5.sp,
                textAlign = TextAlign.End,
                style = LegacyTextStyle
            )
        }
    }
}

@Composable
private fun ColorPickerButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var pressed by remember(label) { mutableStateOf(false) }
    val fillProgress by animateFloatAsState(
        if (pressed) 1f else 0f,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy color picker $label fill"
    )
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        modifier.width(PickerButtonWidth).height(PickerComponentHeight)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { currentOnClick() }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth(fillProgress).fillMaxHeight().background(LegacyPalette.Enabled))
        Text(
            label,
            modifier = Modifier.padding(horizontal = 2.dp),
            color = LegacyPalette.Text,
            fontSize = 8.sp,
            style = LegacyTextStyle
        )
    }
}

private fun ColorRGBA.toComposeColor(opaque: Boolean = false) =
    Color(r, g, b, if (opaque) 255 else a)
