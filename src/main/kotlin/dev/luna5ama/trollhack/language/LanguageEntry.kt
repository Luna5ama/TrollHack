package dev.luna5ama.trollhack.language

import dev.luna5ama.trollhack.interfaces.ILanguageOptionsScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.language.LanguageInfo
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.joml.Vector2i
import java.util.LinkedList

open class LanguageEntry(
    private val refreshListsAction: Runnable,
    private val code: String,
    private val language: LanguageInfo,
    private val selectedLanguages: LinkedList<String>
) : ObjectSelectionList.Entry<LanguageEntry>() {
    private val client = Minecraft.getInstance()

    private val buttons = mutableListOf<AbstractWidget>()
    private val addButton = addButton(15, 24, ADD_TEXTURES, OnPress { add() })
    private val removeButton = addButton(15, 24, REMOVE_TEXTURES, OnPress { remove() })
    private val moveUpButton = addButton(11, 11, MOVE_UP_TEXTURES, OnPress { moveUp() })
    private val moveDownButton =
        addButton(11, 11, MOVE_DOWN_TEXTURES, OnPress { moveDown() })

    private lateinit var parentList: LanguageListWidget

    protected fun addButton(width: Int, height: Int, textures: WidgetSprites, action: OnPress): Button {
        val button = ImageButton(0, 0, width, height, textures, action)
        button.visible = false
        buttons.add(button)
        return button
    }

    private fun isDefault(): Boolean {
        return code == Language.DEFAULT
    }

    private fun isSelected(): Boolean {
        return selectedLanguages.contains(code)
    }

    private fun isFirst(): Boolean {
        return code == selectedLanguages.peekFirst()
    }

    private fun isLast(): Boolean {
        return code == selectedLanguages.peekLast()
    }

    private fun add() {
        if (isFocused) parentList.setFocused(null)
        selectedLanguages.addFirst(code)
        refreshListsAction.run()
    }

    private fun remove() {
        if (isFocused) parentList.setFocused(null)
        selectedLanguages.remove(code)
        refreshListsAction.run()
    }

    fun toggle() {
        if (!isSelected()) add()
        else remove()
    }

    fun moveUp() {
        if (!isSelected()) return
        if (isFirst()) return
        val index = selectedLanguages.indexOf(code)
        selectedLanguages.add(index - 1, selectedLanguages.removeAt(index))
        refreshListsAction.run()
    }

    fun moveDown() {
        if (!isSelected()) return
        if (isLast()) return
        val index = selectedLanguages.indexOf(code)
        selectedLanguages.add(index + 1, selectedLanguages.removeAt(index))
        refreshListsAction.run()
    }

    override fun renderContent(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        hovered: Boolean,
        tickDelta: Float
    ) {
        var y = contentY
        var x = contentX
        val entryWidth = contentWidth
        val entryHeight = contentHeight
        x -= 2
        y -= 2
        if (hovered || isFocused || client.options.touchscreen().get()) {
            context.fill(
                x + 1, y + 1, x + entryWidth - 1, y + entryHeight + 3,
                if ((hovered || isFocused)) -0x5f6f6f70 else 0x50909090
            )
            buttons.forEach { button -> button.visible = false }
            renderButtons(
                ButtonRenderer { button: Button, buttonX: Int, buttonY: Int ->
                    button.x = buttonX
                    button.y = buttonY
                    button.visible = true
                    button.render(context, mouseX, mouseY, tickDelta)
                },
                x, y
            )
            if ((hovered || isFocused) && isDefault()) renderDefaultLanguageTooltip(context, x, y)
        }
        context.drawString(client.font, language.name(), x + 29, y + 3, 0xFFFFFF)
        context.drawString(client.font, language.region(), x + 29, y + 14, 0x808080)
    }

    private fun renderButtons(renderer: ButtonRenderer, x: Int, y: Int) {
        if (isSelected()) {
            renderer.render(removeButton, x, y)
            if (!isFirst()) renderer.render(moveUpButton, x + removeButton.getWidth() + 1, y)
            if (!isLast()) renderer.render(
                moveDownButton,
                x + removeButton.getWidth() + 1,
                y + moveUpButton.getHeight() + 2
            )
        } else renderer.render(addButton, x + 7, y)
    }

    private fun renderDefaultLanguageTooltip(context: GuiGraphics, x: Int, y: Int) {
        val tooltip = client.font.split(DEFAULT_LANGUAGE_TOOLTIP, parentList.rowWidth - 6)
        context.setTooltipForNextFrame(client.font, tooltip, x + 3, y + parentList.rowHeight + 4)
    }

    override fun getNarration(): Component {
        return Component.translatable("narrator.select", language.name)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        for (widget in buttons) if (widget.mouseClicked(event, doubleClick)) {
            (parentList.screen as ILanguageOptionsScreen).languagereload_focusList(parentList)
            return true
        }
        return false
    }

    fun setParent(list: LanguageListWidget) {
        this.parentList = list
    }

    fun getParent(): LanguageListWidget {
        return parentList
    }

    fun getCode(): String {
        return code
    }

    fun getLanguage(): LanguageInfo {
        return language
    }

    private fun interface ButtonRenderer {
        fun render(button: Button, x: Int, y: Int)
    }

    companion object {
        private val DEFAULT_LANGUAGE_TOOLTIP = Component.translatable("language.default.tooltip")

        private val ADD_TEXTURES = WidgetSprites(
            Identifier.tryBuild("trollhack", "language_selection/add")!!,
            Identifier.tryBuild("trollhack", "language_selection/add_highlighted")!!
        )
        private val REMOVE_TEXTURES = WidgetSprites(
            Identifier.tryBuild("trollhack", "language_selection/remove")!!,
            Identifier.tryBuild("trollhack", "language_selection/remove_highlighted")!!
        )
        private val MOVE_UP_TEXTURES = WidgetSprites(
            Identifier.tryBuild("trollhack", "language_selection/move_up")!!,
            Identifier.tryBuild("trollhack", "language_selection/move_up_highlighted")!!
        )
        private val MOVE_DOWN_TEXTURES = WidgetSprites(
            Identifier.tryBuild("trollhack", "language_selection/move_down")!!,
            Identifier.tryBuild("trollhack", "language_selection/move_down_highlighted")!!
        )
    }
}
