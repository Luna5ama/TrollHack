@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.luna5ama.trollhack.gui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.gui.hud.impl.ActiveModules
import dev.luna5ama.trollhack.gui.hud.impl.HudArrayList
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.ClickGui
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import kotlin.math.roundToInt

@Composable
internal fun HudEditorContent() {
    TrollHackCompose.observeRevision()
    val hudModules = ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()
    val modulesByCategory = hudModules.groupBy { it.hudCategory }
    val guiScale = ClickGui.scale / 100f
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press) continue
                    val position = event.changes.firstOrNull()?.position ?: continue
                    val pickerBounds = ClickGuiState.colorPickerBounds
                    if (pickerBounds != null && !pickerBounds.contains(position)) ClickGuiState.closeColorPicker()
                    val settingsBounds = ClickGuiState.settingsBounds
                    if (settingsBounds != null && !settingsBounds.contains(position)) ClickGuiState.closeSettings()
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
                HudCategory.entries.forEach { category ->
                    HudCategoryWindow(category, modulesByCategory[category].orEmpty(), guiScale)
                }
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

        hudModules.filter { it.isEnabled }.forEach { module -> DraggableHudModule(module) }
    }
}

@Composable
private fun HudCategoryWindow(category: HudCategory, modules: List<HudModule>, guiScale: Float) {
    var expanded by remember(category) { mutableStateOf(false) }
    LaunchedEffect(category) { expanded = true }
    val animatedHeight by animateDpAsState(
        if (expanded) 400.dp else 16.dp,
        tween(LegacyWindowDuration, easing = LegacyOutQuart),
        label = "Legacy HUD category height"
    )
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
                category.displayName,
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
            items(modules, key = { it.moduleId }) { module ->
                ModuleEntry(module, guiScale)
            }
        }
    }
}

@Composable
private fun BoxScope.DraggableHudModule(module: HudModule) {
    TrollHackCompose.observeRevision()
    val density = LocalDensity.current.density
    var dragPosition by remember(module) { mutableStateOf(Offset(module._x, module._y)) }
    var moduleSize by remember(module) { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(module._x, module._y) {
        dragPosition = Offset(module._x, module._y)
    }
    Box(
        Modifier.offset {
            IntOffset((dragPosition.x * density).roundToInt(), (dragPosition.y * density).roundToInt())
        }
            .onSizeChanged { moduleSize = it }
            .background(LegacyPalette.WindowStrong)
            .border(0.5.dp, LegacyPalette.Accent)
            .pointerInput(module, density) {
                detectDragGestures(
                    onDragEnd = {
                        val finalPosition = dragPosition
                        mc.execute {
                            module._x = finalPosition.x
                            module._y = finalPosition.y
                            TrollHackCompose.refresh()
                        }
                    },
                    onDragCancel = {
                        dragPosition = Offset(module._x, module._y)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val maxX = (RS.scaledWidthF - moduleSize.width / density).coerceAtLeast(0f)
                    val maxY = (RS.scaledHeightF - moduleSize.height / density).coerceAtLeast(0f)
                    dragPosition = Offset(
                        (dragPosition.x + dragAmount.x / density).coerceIn(0f, maxX),
                        (dragPosition.y + dragAmount.y / density).coerceIn(0f, maxY)
                    )
                }
            }.padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        HudModuleContent(module, editor = true)
    }
}

@Composable
internal fun HudOverlay() {
    TrollHackCompose.observeRevision()
    val density = LocalDensity.current.density
    Box(Modifier.fillMaxSize()) {
        ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()
            .filter { it.isEnabled }
            .forEach { module ->
                Box(
                    Modifier.offset {
                        IntOffset((module._x * density).roundToInt(), (module._y * density).roundToInt())
                    }.background(Color(0x8F101418)).padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    HudModuleContent(module, editor = false)
                }
            }
    }
}

@Composable
private fun HudModuleContent(module: HudModule, editor: Boolean) {
    TrollHackCompose.observeRevision()
    val lines = when (module) {
        is PlainTextHud -> module.lines()
        ActiveModules -> ActiveModules.lines()
        HudArrayList -> HudArrayList.lines()
        Notification -> Notification.messages(showExample = editor)
        else -> listOf(module.nameAsString)
    }.ifEmpty { if (editor) listOf(module.nameAsString) else emptyList() }

    val alignRight = when (module) {
        ActiveModules -> ActiveModules.alignRight()
        HudArrayList -> HudArrayList.alignRight()
        Notification -> Notification.alignRight()
        else -> false
    }
    Column(horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start) {
        lines.forEach { line -> Text(line, color = Color.White, fontSize = 11.sp, maxLines = 1) }
    }
}
