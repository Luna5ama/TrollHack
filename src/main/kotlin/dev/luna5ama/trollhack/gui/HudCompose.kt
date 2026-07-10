package dev.luna5ama.trollhack.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.gui.hud.impl.ActiveModules
import dev.luna5ama.trollhack.gui.hud.impl.HudArrayList
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.Category
import kotlin.math.roundToInt

@Composable
internal fun HudEditorContent() {
    TrollHackCompose.observeRevision()
    val hudModules = ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()
    Box(Modifier.fillMaxSize().background(GuiPalette.Backdrop)) {
        Column(
            Modifier.width(190.dp).fillMaxHeight().background(GuiPalette.PanelStrong).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("HUD EDITOR", color = GuiPalette.Accent, fontSize = 10.sp)
            Text("Layout", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            hudModules.forEach { module ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        .background(GuiPalette.Panel).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(module.nameAsString, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Switch(module.isEnabled, {
                        if (it) module.enable() else module.disable()
                        TrollHackCompose.refresh()
                    })
                }
            }
        }

        hudModules.filter { it.isEnabled }.forEach { module -> DraggableHudModule(module) }
    }
}

@Composable
private fun BoxScope.DraggableHudModule(module: HudModule) {
    TrollHackCompose.observeRevision()
    val density = LocalDensity.current.density
    Box(
        Modifier.offset {
            IntOffset((module._x * density).roundToInt(), (module._y * density).roundToInt())
        }
            .clip(RoundedCornerShape(3.dp)).background(GuiPalette.PanelStrong)
            .border(1.dp, GuiPalette.Accent, RoundedCornerShape(3.dp))
            .pointerInput(module) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    module._x = (module._x + dragAmount.x / density).coerceAtLeast(0f)
                    module._y = (module._y + dragAmount.y / density).coerceAtLeast(0f)
                    TrollHackCompose.refresh()
                }
            }.padding(horizontal = 10.dp, vertical = 6.dp)
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
