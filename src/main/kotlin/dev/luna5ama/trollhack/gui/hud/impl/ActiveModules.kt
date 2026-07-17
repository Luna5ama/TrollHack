package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.gui.HudModule
import dev.luna5ama.trollhack.gui.HudCategory
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.math.vectors.HAlign
import dev.luna5ama.trollhack.utils.math.vectors.VAlign
import org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN

object ActiveModules : HudModule(
    name = "Active Modules",
    description = "List of enabled modules",
    hudCategory = HudCategory.CLIENT
) {
    private var dockingH by setting("Docking H", HAlign.RIGHT)
    private var dockingV by setting("Docking V", VAlign.TOP)
    private val mode by setting("Mode", Mode.LEFT_TAG)
    private val sortingMode by setting("Sorting Mode", SortingMode.LENGTH)
    private val showInvisible by setting("Show Invisible", false)
    private val bindOnly by setting("Bind Only", true, { !showInvisible })
    private val rainbow0 = setting("Rainbow", true)
    private val rainbow by rainbow0
    private val rainbowLength by setting("Rainbow Length", 10.0f, 1.0f..20.0f, 0.5f, { rainbow0.value })
    private val indexedHue by setting("Indexed Hue", 0.5f, 0.0f..1.0f, 0.05f, { rainbow0.value })
    private val saturation by setting("Saturation", 0.5f, 0.0f..1.0f, 0.01f, { rainbow0.value })
    private val brightness by setting("Brightness", 1.0f, 0.0f..1.0f, 0.01f, { rainbow0.value })

    fun lines(): List<String> {
        val modules = ModuleManager.modules.asSequence()
            .filter { it !== this && it.isEnabled }
            .filter { showInvisible || it.isVisible }
            .filter { !bindOnly || it.bind.keyCode != GLFW_KEY_UNKNOWN }
            .toMutableList()

        modules.sortWith(compareBy { sortingKey(it) })
        return modules.map { module ->
            val info = module.getDisplayInfo()?.toString().orEmpty()
            if (info.isBlank()) module.nameAsString else "${module.nameAsString} [$info]"
        }
    }

    fun alignRight(): Boolean = dockingH == HAlign.RIGHT

    private fun sortingKey(module: AbstractModule): String = when (sortingMode) {
        SortingMode.LENGTH -> "%05d".format(99999 - displayLength(module))
        SortingMode.ALPHABET -> module.nameAsString.lowercase()
        SortingMode.CATEGORY -> "%02d-%s".format(Category.entries.indexOf(module.category), module.nameAsString)
    }

    private fun displayLength(module: AbstractModule): Int =
        module.nameAsString.length + (module.getDisplayInfo()?.toString()?.length ?: 0)

    private enum class Mode : Displayable {
        LEFT_TAG,
        RIGHT_TAG,
        FRAME
    }

    private enum class SortingMode(override val displayName: CharSequence) : Displayable {
        LENGTH("Length"),
        ALPHABET("Alphabet"),
        CATEGORY("Category")
    }
}
