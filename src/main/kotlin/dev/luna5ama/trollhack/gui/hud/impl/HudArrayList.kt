package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.config.settings.BooleanSetting
import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.gui.HudCategory
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.manager.managers.GuiManager
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import org.lwjgl.glfw.GLFW

object HudArrayList : HudModule("Array List", hudCategory = HudCategory.CLIENT) {
    private val prefix by setting("Prefix", " [")
    private val suffix by setting("Suffix", "]")
    private val animationLength by setting("Animation Length", 500, 0..1000, 100)
    private val legacyOption by setting("ASSSSSS", true)
    private val lineWidth by setting("Width", 1, 0..5, 1)
    private val renderColor by setting("Color", ColorEnum.RAINBOW)
    private val backgroundAlpha by setting("BackGroundAlpha", 128, 0..255)
    private val appearOnTop by setting("Appear On Top", false)
    private val infoBeforeName by setting("Info First", false)
    private val horizon by setting("Horizon", HorizonAlignment.LEFT)
    private val vertical by setting("Vertical", VerticalAlignment.UP)
    private val sortByLength by setting("Sort By Length", false)
    private val boundOnly by setting("Bound Only", false)
    private val hiddenList = mutableMapOf<Category, BooleanSetting>()

    init {
        Category.entries.associateWith { setting("Hide ${it.displayName}", false) }
            .forEach { (category, setting) -> hiddenList[category] = setting }
    }

    fun lines(): List<String> {
        val hiddenCategories = hiddenList.filterValues { it.value }.keys
        val modules = ModuleManager.modules.asSequence()
            .filterIsInstance<Module>()
            .filter { it.isEnabled && it.category !in hiddenCategories }
            .filter { !boundOnly || it.bind.keyCode != GLFW.GLFW_KEY_UNKNOWN }
            .map { it to renderText(it) }
            .toMutableList()

        if (sortByLength) modules.sortByDescending { it.second.length }
        else modules.sortBy { it.first.nameAsString.lowercase() }
        if (vertical == VerticalAlignment.DOWN) modules.reverse()
        return modules.map { it.second }
    }

    fun alignRight(): Boolean = horizon == HorizonAlignment.RIGHT

    private fun renderText(module: Module): String {
        val info = module.getDisplayInfo()?.toString().orEmpty()
        if (info.isBlank()) return module.nameAsString
        return if (infoBeforeName) "$prefix$info$suffix ${module.nameAsString}"
        else "${module.nameAsString}$prefix$info$suffix"
    }

    private enum class ColorEnum(val colorSupplier: (Int) -> ColorRGBA) : Displayable {
        RAINBOW({ index -> ColorRGBA(GuiManager.getRainbow(index * 100)) }),
        WHITE({ ColorRGBA.WHITE })
    }

    private enum class HorizonAlignment : Displayable { LEFT, RIGHT }
    private enum class VerticalAlignment : Displayable { UP, DOWN }
}
