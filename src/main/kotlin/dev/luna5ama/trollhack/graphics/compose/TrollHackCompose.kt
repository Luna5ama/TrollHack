package dev.luna5ama.trollhack.graphics.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luna5ama.trollhack.config.settings.*
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.skia.SkiaMinecraftBridge
import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.gui.hud.impl.ActiveModules
import dev.luna5ama.trollhack.gui.hud.impl.HudArrayList
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.input.KeyBind
import kotlin.math.roundToInt

object TrollHackCompose {
    enum class Mode { NONE, CLICK_GUI, HUD_EDITOR }

    var mode by mutableStateOf(Mode.NONE)
        private set
    private var revision by mutableIntStateOf(0)
    private var bindTarget: BindSetting? = null
    private var lastSync = 0L

    fun start() {
        SkiaMinecraftBridge.setComposeContent { TrollHackRoot() }
    }

    fun show(mode: Mode) {
        this.mode = mode
        refresh()
    }

    fun hide() {
        mode = Mode.NONE
        bindTarget = null
        refresh()
    }

    fun beginBind(setting: BindSetting) {
        bindTarget = setting
        refresh()
    }

    fun consumeBind(keyCode: Int, scanCode: Int): Boolean {
        val setting = bindTarget ?: return false
        setting.value = if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE ||
            keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
        ) KeyBind.NONE else KeyBind(KeyBind.Category.KEYBOARD, keyCode, scanCode)
        bindTarget = null
        refresh()
        return true
    }

    fun isBinding(setting: BindSetting) = bindTarget === setting

    fun refresh() {
        revision++
    }

    fun syncFrame() {
        val now = System.nanoTime()
        if (now - lastSync >= 50_000_000L) {
            lastSync = now
            revision++
        }
    }

    @Composable
    private fun TrollHackRoot() {
        revision
        TrollHackTheme {
            when (mode) {
                Mode.CLICK_GUI -> ClickGuiContent()
                Mode.HUD_EDITOR -> HudEditorContent()
                Mode.NONE -> if (mc.screen == null && mc.level != null) HudOverlay()
            }
        }
    }
}

@Composable
private fun TrollHackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors(
            primary = Color(0xFF5EC6B7),
            primaryVariant = Color(0xFF389B90),
            secondary = Color(0xFFF0A35A),
            background = Color(0xFF111418),
            surface = Color(0xFF1A1F24),
            onPrimary = Color(0xFF071512),
            onSecondary = Color(0xFF211207),
            onBackground = Color(0xFFE7EBEE),
            onSurface = Color(0xFFE7EBEE)
        ),
        typography = Typography(defaultFontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
            content()
        }
    }
}

@Composable
private fun ClickGuiContent() {
    var selectedCategory by remember { mutableStateOf(Category.COMBAT as Category) }
    var selectedModule by remember { mutableStateOf<AbstractModule?>(null) }
    var search by remember { mutableStateOf("") }
    val modules = ModuleManager.getModulesByCategory(selectedCategory).filter {
        search.isBlank() || it.nameAsString.contains(search, ignoreCase = true) ||
            it.description.toString().contains(search, ignoreCase = true)
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xB30A0D10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colors.background)
                .border(1.dp, Color(0xFF394149), RoundedCornerShape(6.dp))
        ) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF161B20)).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TrollHack", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("  Compose Console", color = MaterialTheme.colors.primary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                SearchField(search) { search = it }
            }

            Row(Modifier.fillMaxSize()) {
                CategoryRail(selectedCategory) {
                    selectedCategory = it
                    selectedModule = null
                }
                ModuleList(modules, selectedModule) { selectedModule = it }
                SettingsPanel(selectedModule)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Box(
        Modifier.width(230.dp).height(34.dp).clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF252B31)).border(1.dp, Color(0xFF3A434B), RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text("Search modules", color = Color(0xFF8D969E), fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CategoryRail(selected: Category, onSelect: (Category) -> Unit) {
    Column(
        Modifier.width(150.dp).fillMaxHeight().background(Color(0xFF151A1F)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("CATEGORIES", color = Color(0xFF7F8992), fontSize = 10.sp, modifier = Modifier.padding(8.dp))
        Category.entries.forEach { category ->
            val active = category == selected
            Row(
                Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (active) Color(0xFF263A39) else Color.Transparent)
                    .clickable { onSelect(category) }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(3.dp, 18.dp).background(if (active) MaterialTheme.colors.primary else Color.Transparent))
                Spacer(Modifier.width(9.dp))
                Text(category.displayName.toString(), color = if (active) Color.White else Color(0xFFB6BEC5), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ModuleList(
    modules: List<AbstractModule>,
    selected: AbstractModule?,
    onSelect: (AbstractModule) -> Unit
) {
    LazyColumn(
        Modifier.width(330.dp).fillMaxHeight().background(Color(0xFF12161A)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(modules, key = { it.moduleId }) { module ->
            val active = selected === module
            Row(
                Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (active) Color(0xFF222A30) else Color(0xFF191E23))
                    .border(1.dp, if (active) Color(0xFF49736E) else Color(0xFF292F35), RoundedCornerShape(6.dp))
                    .clickable { onSelect(module) }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(module.nameAsString, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (module.description.isNotEmpty()) {
                        Text(module.description.toString(), color = Color(0xFF929CA5), fontSize = 10.sp, maxLines = 1)
                    }
                }
                Switch(
                    checked = module.isEnabled,
                    onCheckedChange = {
                        if (it) module.enable() else module.disable()
                        TrollHackCompose.refresh()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                )
            }
        }
    }
}

@Composable
private fun RowScope.SettingsPanel(module: AbstractModule?) {
    Column(Modifier.weight(1f).fillMaxHeight().padding(18.dp)) {
        if (module == null) {
            Text("Select a module", color = Color(0xFF8D969E), fontSize = 15.sp)
            return@Column
        }
        Text(module.nameAsString, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("SETTINGS", color = MaterialTheme.colors.secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            module.filteredSettings.filter { it.isVisible }.forEach { SettingRow(it) }
        }
    }
}

@Composable
private fun SettingRow(setting: AbstractSetting<*, *>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF1A1F24))
            .border(1.dp, Color(0xFF2D343A), RoundedCornerShape(5.dp)).padding(10.dp)
    ) {
        when (setting) {
            is BooleanSetting -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingLabel(setting, Modifier.weight(1f))
                    Switch(setting.value, {
                        setting.value = it
                        TrollHackCompose.refresh()
                    })
                }
            }
            is AbstractRangedSetting<*, *> -> RangedSetting(setting)
            is EnumSetting<*> -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingLabel(setting, Modifier.weight(1f))
                    OutlinedButton(onClick = { setting.next(); TrollHackCompose.refresh() }) {
                        Text((setting.value as? Displayable)?.displayName?.toString() ?: setting.value.toString(), fontSize = 11.sp)
                    }
                }
            }
            is StringSetting -> {
                SettingLabel(setting)
                BasicTextField(
                    value = setting.value,
                    onValueChange = { setting.value = it; TrollHackCompose.refresh() },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).background(Color(0xFF252B31)).padding(8.dp)
                )
            }
            is ColorSetting -> ColorSettingRow(setting)
            is BindSetting -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingLabel(setting, Modifier.weight(1f))
                    OutlinedButton(onClick = { TrollHackCompose.beginBind(setting) }) {
                        Text(if (TrollHackCompose.isBinding(setting)) "Press a key" else setting.keyName, fontSize = 11.sp)
                    }
                }
            }
            is LabelSetting -> Text(setting.label(), color = Color(0xFFBBC3C9), fontSize = 12.sp)
            else -> SettingLabel(setting)
        }
    }
}

@Composable
private fun SettingLabel(setting: AbstractSetting<*, *>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(setting.nameAsString, fontSize = 13.sp)
        if (setting.description.isNotEmpty()) Text(setting.description, color = Color(0xFF89939B), fontSize = 10.sp)
    }
}

@Composable
private fun RangedSetting(setting: AbstractRangedSetting<*, *>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingLabel(setting, Modifier.weight(1f))
        Text(formatSettingValue(setting.value), color = MaterialTheme.colors.primary, fontSize = 11.sp)
    }
    Slider(
        value = settingRatio(setting),
        onValueChange = { setRangedSetting(setting, it); TrollHackCompose.refresh() },
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colors.primary, activeTrackColor = MaterialTheme.colors.primary)
    )
}

@Composable
private fun ColorSettingRow(setting: ColorSetting) {
    val color = setting.value
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingLabel(setting, Modifier.weight(1f))
        Box(
            Modifier.size(30.dp, 18.dp).background(Color(color.r, color.g, color.b, color.a))
                .border(1.dp, Color.White.copy(alpha = 0.5f))
        )
    }
    listOf("R" to color.r, "G" to color.g, "B" to color.b, "A" to color.a).forEachIndexed { index, pair ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pair.first, modifier = Modifier.width(18.dp), fontSize = 10.sp)
            Slider(
                value = pair.second / 255f,
                onValueChange = { ratio ->
                    val channel = (ratio * 255f).roundToInt()
                    setting.value = when (index) {
                        0 -> color.red(channel)
                        1 -> color.green(channel)
                        2 -> color.blue(channel)
                        else -> color.alpha(channel)
                    }
                    TrollHackCompose.refresh()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HudEditorContent() {
    val hudModules = ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()
    Box(Modifier.fillMaxSize().background(Color(0x99080B0E))) {
        Column(
            Modifier.width(190.dp).fillMaxHeight().background(Color(0xEE151A1F)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("HUD EDITOR", color = MaterialTheme.colors.primary, fontSize = 11.sp)
            Text("Layout", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            hudModules.forEach { module ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF20262C)).padding(8.dp),
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
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(
        Modifier.offset {
            IntOffset((module._x * density).roundToInt(), (module._y * density).roundToInt())
        }
            .clip(RoundedCornerShape(4.dp)).background(Color(0xDD20272D))
            .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(4.dp))
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
private fun HudOverlay() {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(Modifier.fillMaxSize()) {
        ModuleManager.getModulesByCategory(Category.HUD).filterIsInstance<HudModule>()
            .filter { it.isEnabled }
            .forEach { module ->
                Box(
                    Modifier.offset {
                        IntOffset((module._x * density).roundToInt(), (module._y * density).roundToInt())
                    }
                        .background(Color(0x8F101418)).padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    HudModuleContent(module, editor = false)
                }
            }
    }
}

@Composable
private fun HudModuleContent(module: HudModule, editor: Boolean) {
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
        lines.forEach { line ->
            Text(line, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun settingRatio(setting: AbstractRangedSetting<*, *>): Float = when (val value = setting.value) {
    is Int -> ((value - setting.range.start as Int).toFloat() / ((setting.range.endInclusive as Int) - setting.range.start as Int)).coerceIn(0f, 1f)
    is Long -> ((value - setting.range.start as Long).toFloat() / ((setting.range.endInclusive as Long) - setting.range.start as Long)).coerceIn(0f, 1f)
    is Float -> ((value - setting.range.start as Float) / ((setting.range.endInclusive as Float) - setting.range.start as Float)).coerceIn(0f, 1f)
    is Double -> ((value - setting.range.start as Double) / ((setting.range.endInclusive as Double) - setting.range.start as Double)).toFloat().coerceIn(0f, 1f)
    else -> 0f
}

@Suppress("UNCHECKED_CAST")
private fun setRangedSetting(setting: AbstractRangedSetting<*, *>, ratio: Float) {
    fun lerp(start: Double, end: Double) = start + (end - start) * ratio
    when (setting.value) {
        is Int -> (setting as AbstractRangedSetting<Int, *>).value = lerp(setting.range.start.toDouble(), setting.range.endInclusive.toDouble()).roundToInt()
        is Long -> (setting as AbstractRangedSetting<Long, *>).value = lerp(setting.range.start.toDouble(), setting.range.endInclusive.toDouble()).toLong()
        is Float -> (setting as AbstractRangedSetting<Float, *>).value = lerp(setting.range.start.toDouble(), setting.range.endInclusive.toDouble()).toFloat()
        is Double -> (setting as AbstractRangedSetting<Double, *>).value = lerp(setting.range.start, setting.range.endInclusive)
    }
}

private fun formatSettingValue(value: Any?) = when (value) {
    is Float -> "%.2f".format(value)
    is Double -> "%.2f".format(value)
    else -> value.toString()
}
