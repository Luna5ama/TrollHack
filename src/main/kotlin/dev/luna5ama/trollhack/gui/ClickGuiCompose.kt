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
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import kotlin.math.roundToInt

internal object ClickGuiState {
    var search by mutableStateOf("")
    var settingsModule by mutableStateOf<AbstractModule?>(null)
    var settingsPosition by mutableStateOf(Offset.Zero)
    var settingsBounds by mutableStateOf<Rect?>(null)
    var textInputFocused by mutableStateOf(false)

    fun openSettings(module: AbstractModule, position: Offset) {
        if (settingsModule === module) {
            closeSettings()
        } else {
            settingsModule = module
            settingsPosition = position
            settingsBounds = null
        }
    }

    fun closeSettings() {
        settingsModule = null
        settingsPosition = Offset.Zero
        settingsBounds = null
        textInputFocused = false
    }

    fun handleEscape(): Boolean {
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
    Category.VISUAL,
    Category.PLAYER,
    Category.MOVEMENT,
    Category.MISC,
    Category.COMBAT,
    Category.CLIENT,
    Category.HUD
)

@Composable
internal fun ClickGuiContent() {
    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press || ClickGuiState.settingsModule == null) continue
                    val position = event.changes.firstOrNull()?.position ?: continue
                    val bounds = ClickGuiState.settingsBounds
                    if (bounds != null && !bounds.contains(position)) ClickGuiState.closeSettings()
                }
            }
        }
    ) {
        Row(
            Modifier.offset(50.dp, 65.dp).height(400.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            legacyCategoryOrder.forEach { category -> CategoryWindow(category) }
        }

        ClickGuiState.settingsModule?.let { module ->
            ModuleSettingsWindow(module, constraints.maxWidth, constraints.maxHeight)
        }
    }
}

@Composable
private fun CategoryWindow(category: Category) {
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
            Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                legacyCategoryName(category),
                modifier = Modifier.weight(1f),
                color = LegacyPalette.Text,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(modules, key = { it.moduleId }) { module -> ModuleEntry(module) }
        }
    }
}

@Composable
private fun ModuleEntry(module: AbstractModule) {
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
                                    Offset(rootPosition.x + rowWidth + 2.dp.toPx(), rootPosition.y)
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
            Modifier.fillMaxSize().padding(horizontal = 4.dp).background(
                if (selected) LegacyPalette.Selected else Color.Transparent
            )
        ) {
            Box(Modifier.fillMaxWidth(fillProgress).fillMaxHeight().background(LegacyPalette.Enabled))
        }
        Text(
            module.nameAsString,
            modifier = Modifier.padding(horizontal = 5.dp),
            color = LegacyPalette.Text,
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun BoxScope.ModuleSettingsWindow(module: AbstractModule, viewportWidth: Int, viewportHeight: Int) {
    TrollHackCompose.observeRevision()
    val density = LocalDensity.current.density
    val visibleCount = module.filteredSettings.count { it.isVisible }
    val widthDp = 122.dp
    val heightDp = (16 + visibleCount.coerceIn(1, 22) * 13).dp
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
                    }.padding(start = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    module.nameAsString,
                    color = LegacyPalette.Text,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
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
                Text("x", color = LegacyPalette.TextMuted, fontSize = 8.sp)
            }
        }
        SettingsList(module)
    }
}

internal object LegacyPalette {
    val Accent = Color(0xFFD86E96)
    val Enabled = Color(0xFFE277A0)
    val Selected = Color(0x664B2836)
    val Window = Color(0xD9231D20)
    val WindowStrong = Color(0xF0272024)
    val Row = Color(0x9E251D21)
    val RowAlt = Color(0xB31D181B)
    val Track = Color(0xFF55454C)
    val Text = Color(0xFFF4F1F2)
    val TextMuted = Color(0xFFC2B8BC)
}

private fun legacyCategoryName(category: Category) = when (category) {
    Category.VISUAL -> "Render"
    Category.COMBAT -> "Combat"
    Category.MISC -> "Misc"
    else -> category.displayName.toString()
}
