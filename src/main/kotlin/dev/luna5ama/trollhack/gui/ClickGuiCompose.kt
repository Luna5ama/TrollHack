@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.luna5ama.trollhack.gui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.config.settings.ColorSetting
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.ClickGui
import kotlin.math.roundToInt

internal object ClickGuiState {
    var search by mutableStateOf("")
    var settingsModule by mutableStateOf<AbstractModule?>(null)
    var settingsPosition by mutableStateOf(Offset.Zero)
    var settingsBounds by mutableStateOf<Rect?>(null)
    var colorPickerSetting by mutableStateOf<ColorSetting?>(null)
    var colorPickerBounds by mutableStateOf<Rect?>(null)
    var textInputFocused by mutableStateOf(false)

    fun openSettings(module: AbstractModule, position: Offset) {
        if (settingsModule === module) {
            closeSettings()
        } else {
            closeColorPicker()
            settingsModule = module
            settingsPosition = position
            settingsBounds = null
        }
    }

    fun openColorPicker(setting: ColorSetting) {
        colorPickerSetting = setting
        colorPickerBounds = null
        textInputFocused = false
    }

    fun closeColorPicker() {
        colorPickerSetting = null
        colorPickerBounds = null
    }

    fun closeSettings() {
        closeColorPicker()
        settingsModule = null
        settingsPosition = Offset.Zero
        settingsBounds = null
        textInputFocused = false
    }

    fun handleEscape(): Boolean {
        if (colorPickerSetting != null) {
            closeColorPicker()
            return true
        }
        if (settingsModule != null) {
            closeSettings()
            return true
        }
        if (search.isNotEmpty()) {
            search = ""
            return true
        }
        return false
    }
}

private val legacyCategoryOrder = listOf(
    Category.COMBAT,
    Category.MISC,
    Category.PLAYER,
    Category.MOVEMENT,
    Category.RENDER,
    Category.CLIENT,
)

@Composable
internal fun ClickGuiContent() {
    TrollHackCompose.observeRevision()
    val guiScale = ClickGui.scale / 100f
    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press) continue
                    val position = event.changes.firstOrNull()?.position ?: continue

                    if (ClickGuiState.colorPickerSetting != null) {
                        val pickerBounds = ClickGuiState.colorPickerBounds ?: continue
                        if (pickerBounds.contains(position)) continue
                        ClickGuiState.closeColorPicker()
                    }

                    if (ClickGuiState.settingsModule != null) {
                        val settingsBounds = ClickGuiState.settingsBounds
                        if (settingsBounds != null && !settingsBounds.contains(position)) {
                            ClickGuiState.closeSettings()
                        }
                    }
                }
            }
        }
    ) {
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        val density = LocalDensity.current
        val logicalWidth = with(density) { (viewportWidth / guiScale).toDp() }
        val logicalHeight = with(density) { (viewportHeight / guiScale).toDp() }
        Box(
            Modifier.width(logicalWidth).height(logicalHeight).graphicsLayer {
                scaleX = guiScale
                scaleY = guiScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        ) {
            Row(
                Modifier.offset(50.dp, 65.dp).height(400.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                legacyCategoryOrder.forEach { category -> CategoryWindow(category, guiScale) }
            }

            ClickGuiState.settingsModule?.let { module ->
                ModuleSettingsWindow(
                    module,
                    (viewportWidth / guiScale).roundToInt(),
                    (viewportHeight / guiScale).roundToInt()
                )
            }

            ClickGuiState.colorPickerSetting?.let { setting ->
                ColorPickerWindow(
                    setting,
                    (viewportWidth / guiScale).roundToInt(),
                    (viewportHeight / guiScale).roundToInt()
                )
            }
        }
    }
}

@Composable
private fun CategoryWindow(category: Category, guiScale: Float) {
    TrollHackCompose.observeRevision()
    var expanded by remember(category) { mutableStateOf(false) }
    LaunchedEffect(category) { expanded = true }
    val animatedHeight by animateDpAsState(
        if (expanded) 400.dp else 16.dp,
        tween(LegacyWindowDuration, easing = LegacyOutQuart),
        label = "Legacy category height"
    )
    val modules = ModuleManager.getModulesByCategory(category).filter {
        ClickGuiState.search.isBlank() || it.nameAsString.contains(ClickGuiState.search, true) ||
            it.alias.any { alias -> alias.contains(ClickGuiState.search, true) }
    }

    Column(
        Modifier.width(80.dp).height(animatedHeight).clipToBounds()
            .background(LegacyPalette.Window)
            .border(0.5.dp, LegacyPalette.Accent)
    ) {
        Row(
            Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                legacyCategoryName(category),
                modifier = Modifier.weight(1f),
                color = LegacyPalette.Text,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = LegacyTextStyle
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(modules, key = { it.moduleId }) { module -> ModuleEntry(module, guiScale) }
        }
    }
}

@Composable
internal fun ModuleEntry(module: AbstractModule, guiScale: Float) {
    TrollHackCompose.observeRevision()
    var rootPosition by remember(module) { mutableStateOf(Offset.Zero) }
    var rowWidth by remember(module) { mutableStateOf(0) }
    val selected = ClickGuiState.settingsModule === module
    var fillTarget by remember(module) { mutableStateOf(0f) }
    LaunchedEffect(module.isEnabled) { fillTarget = if (module.isEnabled) 1f else 0f }
    val fillProgress by animateFloatAsState(
        fillTarget,
        tween(LegacyFillDuration, easing = LegacyOutQuart),
        label = "Legacy module fill"
    )

    Box(
        Modifier.fillMaxWidth().height(13.dp)
            .onGloballyPositioned {
                rootPosition = it.positionInRoot()
                rowWidth = it.size.width
            }
            .pointerInput(module) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Press) continue
                        when (event.button) {
                            PointerButton.Primary -> {
                                module.toggle()
                                TrollHackCompose.refresh()
                            }
                            PointerButton.Secondary -> {
                                ClickGuiState.openSettings(
                                    module,
                                    Offset(
                                        rootPosition.x / guiScale + rowWidth + 2.dp.toPx(),
                                        rootPosition.y / guiScale
                                    )
                                )
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier.fillMaxWidth().height(12.dp).padding(horizontal = 4.dp).background(
                if (selected) LegacyPalette.Selected else Color.Transparent
            )
        ) {
            Box(Modifier.fillMaxWidth(fillProgress).fillMaxHeight().background(LegacyPalette.Enabled))
        }
        Text(
            module.nameAsString,
            modifier = Modifier.padding(horizontal = 6.dp),
            color = LegacyPalette.Text,
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = LegacyTextStyle
        )
    }
}

@Composable
internal fun BoxScope.ModuleSettingsWindow(module: AbstractModule, viewportWidth: Int, viewportHeight: Int) {
    TrollHackCompose.observeRevision()
    val density = LocalDensity.current.density
    val visibleCount = module.filteredSettings.count { it.isVisible }
    val widthDp = 122.dp
    val heightDp = (20 + visibleCount.coerceIn(1, 22) * 13).dp
    var opening by remember(module) { mutableStateOf(true) }
    var heightTarget by remember(module) { mutableStateOf(16.dp) }
    LaunchedEffect(module) {
        heightTarget = heightDp
        kotlinx.coroutines.delay(LegacyWindowDuration.toLong())
        opening = false
    }
    LaunchedEffect(heightDp, opening) {
        if (!opening) heightTarget = heightDp
    }
    val animatedHeight by animateDpAsState(
        heightTarget,
        tween(
            if (opening) LegacyWindowDuration else LegacyComponentDuration,
            easing = if (opening) LegacyOutQuart else LegacyOutCubic
        ),
        label = "Legacy setting window height"
    )
    val widthPx = widthDp.value * density
    val heightPx = heightDp.value * density
    val x = ClickGuiState.settingsPosition.x.coerceIn(2f, (viewportWidth - widthPx - 2f).coerceAtLeast(2f))
    val y = ClickGuiState.settingsPosition.y.coerceIn(2f, (viewportHeight - heightPx - 2f).coerceAtLeast(2f))
    val animatedPosition by animateOffsetAsState(
        Offset(x, y),
        tween(LegacyComponentDuration, easing = LegacyOutCubic),
        label = "Legacy setting window position"
    )

    Column(
        Modifier.offset { IntOffset(animatedPosition.x.roundToInt(), animatedPosition.y.roundToInt()) }
            .width(widthDp).height(animatedHeight).clipToBounds()
            .background(LegacyPalette.WindowStrong)
            .border(0.5.dp, LegacyPalette.Accent)
            .onGloballyPositioned { ClickGuiState.settingsBounds = it.boundsInRoot() }
    ) {
        Row(
            Modifier.fillMaxWidth().height(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .pointerInput(module) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            ClickGuiState.settingsPosition += amount
                        }
                    }.padding(start = 3.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    module.nameAsString,
                    color = LegacyPalette.Text,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = LegacyTextStyle
                )
            }
            Box(
                Modifier.size(14.dp).pointerInput(module) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.button == PointerButton.Primary) {
                                event.changes.forEach { it.consume() }
                                ClickGuiState.closeSettings()
                            }
                        }
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Text("x", color = LegacyPalette.TextMuted, fontSize = 8.sp, style = LegacyTextStyle)
            }
        }
        SettingsList(module)
    }
}

internal object LegacyPalette {
    val Accent = Color(255, 140, 180, 220)
    val Enabled = Accent
    val Selected = Color(255, 255, 255, 32)
    val Window = Color(40, 32, 36, 160)
    val WindowStrong = Window
    val Row = Color.Transparent
    val RowAlt = Color.Transparent
    val Track = Color(255, 255, 255, 32)
    val Text = Color(255, 250, 253, 255)
    val TextMuted = Color(255, 250, 253, 190)
}

private fun legacyCategoryName(category: Category) = category.displayString
